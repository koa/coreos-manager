package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.yaml.snakeyaml.Yaml;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.service.GitAccessService;
import ch.bergturbenthal.coreos.manager.service.KeyGenerator;
import ch.bergturbenthal.coreos.manager.service.PropertiesService;
import ch.bergturbenthal.coreos.manager.service.ProxyService;
import lombok.Cleanup;

@Service
public class DefaultConfigurationService implements ConfigurationService {

	private static interface Converter {
		Resource convert(Resource value);
	}

	public final class TemplateUtils {
		private final String mac;

		public TemplateUtils(final String mac) {
			this.mac = mac;
		}

		public String joinPattern(final String column, final String key, final String pattern) {
			final Matcher matcher = Pattern.compile("\\{[a-zA-Z0-9_-]+\\}").matcher(pattern);
			final List<String> patternResult = new ArrayList<String>();
			int lastEnd = 0;
			while (matcher.find()) {
				final int start = matcher.start();
				patternResult.add(pattern.substring(lastEnd, start));
				lastEnd = matcher.end();
				patternResult.add(pattern.substring(start + 1, lastEnd - 1));
			}
			patternResult.add(pattern.substring(lastEnd));
			return propertiesService.allPropertiesOfKey(column, key).map((Function<Map<String, String>, String>) t -> {
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < patternResult.size(); i++) {
					final String patternPart = patternResult.get(i);
					if (i % 2 == 0) {
						sb.append(patternPart);
					} else {
						sb.append(t.get(patternPart));
					}
				}
				return sb.toString();
			}).collect(Collectors.joining(","));
		}

		public String load(final String file) throws IOException {
			final Resource loadedFile = generateFile(file, mac);
			@Cleanup
			final InputStream inputStream = loadedFile.getInputStream();
			return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		}

		public int oct2dec(final int octAsInt) {
			return Integer.parseInt(Integer.toString(octAsInt), 8);
		}

		public String proxyFor(final String uri) {
			return resolve("genproxy/" + proxyService.createHandle(URI.create(uri)));
		}

		public String resolve(final String relative) {
			final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			return UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).replacePath(request.getContextPath()).path(relative).toUriString();
		}

		public String resolvePlain(final String relative) {
			final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			return UriComponentsBuilder	.fromHttpRequest(new ServletServerHttpRequest(request))
																	.replacePath(request.getContextPath())
																	.path(relative)
																	.replaceQuery(null)
																	.toUriString();
		}

		public String source(final String contentType, final String path) throws IOException {
			final Resource resource = generateFile(path, mac);
			final InputStreamReader reader = new InputStreamReader(new Base64InputStream(resource.getInputStream(), true, -1, null), StandardCharsets.UTF_8);
			return "data:" + contentType + ";base64," + IOUtils.toString(reader);
		}

		public String sourceEncode(final String contentType, final String data) throws IOException {
			return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static Converter DEFAULT_CONVERTER = new Converter() {

		@Override
		public Resource convert(final Resource value) {
			return value;
		}
	};
	private static final String JSON_ENDING = ".json";

	private static Converter YAML_JSON_CONVERTER = new Converter() {

		@Override
		public Resource convert(final Resource value) {
			try {
				final Yaml yaml = new Yaml();
				final Map<String, Object> data = (Map<String, Object>) yaml.load(value.getInputStream());
				final JSONObject jsonObject = new JSONObject(data);
				return new ByteArrayResource(jsonObject.toString(2).getBytes(StandardCharsets.UTF_8));
			} catch (final IOException e) {
				throw new RuntimeException("Cannot convert resource " + value + " to json", e);
			}
		}
	};

	private final ThreadLocal<Context> currentRunningContext = new ThreadLocal<>();
	private final GitAccessService gitAccessService;
	private final KeyGenerator keyGenerator;
	private final PropertiesService propertiesService;
	private final ProxyService proxyService;

	@Autowired
	public DefaultConfigurationService(	final PropertiesService propertiesService, final GitAccessService gitAccessService, final KeyGenerator keyGenerator,
																			final ProxyService proxyService) {
		this.propertiesService = propertiesService;
		this.gitAccessService = gitAccessService;
		this.keyGenerator = keyGenerator;
		this.proxyService = proxyService;
		final RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
		runtimeServices.addProperty(RuntimeConstants.RESOURCE_LOADER, "delegate");
		runtimeServices.addProperty("delegate.resource.loader.class", DelegatingResourceLoader.class.getName());
	}

	@Override
	public Resource generateFile(final String relativePath, final String mac) throws IOException {
		final Map<String, Converter> fileCandidates = new LinkedHashMap<String, Converter>();
		fileCandidates.put(relativePath, DEFAULT_CONVERTER);
		if (relativePath.endsWith(JSON_ENDING)) {
			final String yamlFile = relativePath.subSequence(0, relativePath.length() - JSON_ENDING.length()) + ".yml";
			fileCandidates.put(yamlFile, YAML_JSON_CONVERTER);
		}

		final Map<String, ObjectLoader> foundFiles = gitAccessService.findFiles(fileCandidates.keySet());
		for (final Entry<String, Converter> fileEntry : fileCandidates.entrySet()) {
			final ObjectLoader foundLoader = foundFiles.get(fileEntry.getKey());
			if (foundLoader == null) {
				continue;
			}
			return fileEntry.getValue().convert(loadTemplate(foundLoader, relativePath, mac, new TemplateUtils(mac)));
		}
		throw new FileNotFoundException("none of " + fileCandidates.keySet() + " found");
	}

	@Override
	public Resource generatePXE(final String mac) throws IOException {
		final String pxeFile = "pxe/" + propertiesService.valuesOfKey("mac", mac, "pxe").findFirst().orElse("default") + ".pxe";
		return generateFile(pxeFile, mac);

	}

	private Resource loadTemplate(final ObjectLoader loader, final String templateName, final String mac, final Object utils) throws IOException {

		final Context currentContext = currentRunningContext.get();
		if (currentContext != null) {
			return processInternal(loader, templateName, currentContext);
		}
		final Context context = new VelocityContext();
		context.put("mac", mac);
		context.put("props", propertiesService);
		context.put("util", utils);
		context.put("keyGenerator", keyGenerator);
		currentRunningContext.set(context);
		try {
			return processInternal(loader, templateName, context);
		} finally {
			currentRunningContext.set(null);
		}
	}

	private Resource processInternal(final ObjectLoader loader, final String templateName, final Context context) throws MissingObjectException, IOException {
		@Cleanup
		final InputStream profileStream = loader.openStream();

		final ResourceLoader manager = new ResourceLoader() {

			@Override
			public long getLastModified(final org.apache.velocity.runtime.resource.Resource resource) {
				return gitAccessService.getLastModified();
			}

			@Override
			public InputStream getResourceStream(final String source) throws ResourceNotFoundException {
				try {
					return gitAccessService	.findFiles(Collections.singleton(source))
																	.values()
																	.stream()
																	.findFirst()
																	.orElseThrow(() -> new ResourceNotFoundException(source + " not found"))
																	.openStream();
				} catch (final IOException e) {
					throw new ResourceNotFoundException("Cannot access resource " + source, e);
				}
			}

			@Override
			public void init(final ExtendedProperties configuration) {
				// ignore
			}

			@Override
			public boolean isSourceModified(final org.apache.velocity.runtime.resource.Resource resource) {
				return resource.getLastModified() != gitAccessService.getLastModified();
			}
		};
		return DelegatingResourceLoader.callWithLoader(new Callable<Resource>() {

			@Override
			public Resource call() throws Exception {
				final StringWriter writer = new StringWriter();
				Velocity.evaluate(context, writer, "template-" + templateName, new InputStreamReader(profileStream, Charsets.UTF_8));
				writer.flush();
				return new ByteArrayResource(writer.toString().getBytes(Charsets.UTF_8));
			}
		}, manager);
	}

}
