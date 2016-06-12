package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.profile.ProfileResult;
import ch.bergturbenthal.coreos.manager.profile.ProfileSpecification;
import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.service.ProfileResolver;
import ch.bergturbenthal.coreos.manager.service.TemplateLoader;

@Service
public class DefaultConfigurationService implements ConfigurationService {

	private final URI ignitionBase;
	private final ProfileResolver profileResolver;
	private final Map<String, List<String>> properties;
	private final TemplateLoader templateLoader;

	@Autowired
	public DefaultConfigurationService(final ProfileResolver profileResolver, final Configuration configuration, final TemplateLoader templateLoader) {
		this.profileResolver = profileResolver;
		this.templateLoader = templateLoader;
		ignitionBase = configuration.getIgnitionBase();
		properties = configuration.getProperties();
	}

	private Map<String, String[]> fillProperties(final Map<String, String[]> parameterMap) {
		final Map<String, String[]> ret = new HashMap<String, String[]>(parameterMap);
		if (properties != null) {
			properties.forEach((key, value) -> {
				ret.merge(key, value.toArray(new String[value.size()]), (BiFunction<String[], String[], String[]>) (t, u) -> ArrayUtils.addAll(t, u));
			});
		}
		return ret;
	}

	@Override
	public String generateIgnition(final Map<String, String[]> parameterMap) throws IOException {

		final ProfileResult resolvedProfile = profileResolver.resolveProfile(fillProperties(parameterMap));
		final ProfileSpecification spec = resolvedProfile.getSpecification();
		final String ignitionTemplate = spec.getIgnitionTemplate();
		final String yamlTemplate = templateLoader.loadTemplate(ignitionBase.resolve(ignitionTemplate + ".yml").toURL(), ignitionTemplate, resolvedProfile.getProperties());
		final Yaml yaml = new Yaml();
		final Map<String, Object> data = (Map<String, Object>) yaml.load(yamlTemplate);
		final JSONObject jsonObject = new JSONObject(data);
		return jsonObject.toString();
	}

	@Override
	public String generatePXE(final Map<String, String[]> parameterMap) throws IOException {
		final ProfileResult resolvedProfile = profileResolver.resolveProfile(fillProperties(parameterMap));
		final ProfileSpecification spec = resolvedProfile.getSpecification();
		final String channel = resolvedProfile.getSpecification().getChannel();
		final StringBuilder output = new StringBuilder("#!ipxe\nkernel /kernel/");
		output.append(channel);
		final Map<String, String[]> kernelParameters = spec.getKernelParameters();

		if (kernelParameters != null) {
			kernelParameters.forEach((key, valueList) -> {
				if (valueList == null) {
					output.append(" ");
					output.append(key);
				} else {
					Stream.of(valueList).forEach(v -> {
						output.append(" ");
						output.append(key);
						if (v != null && !v.isEmpty()) {
							output.append("=");
							output.append(v);
						}
					});
				}
			});
		}

		output.append("\ninitrd /initrd/");
		output.append(channel);
		output.append("\nboot\n");
		return output.toString();
	}

}
