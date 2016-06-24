package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.Charsets;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import ch.bergturbenthal.coreos.manager.service.PropertiesService;
import ch.bergturbenthal.coreos.manager.service.TemplateLoader;
import lombok.Cleanup;

@Service
public class DefaultTemplateLoader implements TemplateLoader {
	public static final class UrlResolver {
		private final String mac;

		public UrlResolver(final String mac) {
			this.mac = mac;
		}

		public String resolve(final String relative) {
			final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			return UriComponentsBuilder	.fromHttpRequest(new ServletServerHttpRequest(request))
																	.replacePath(request.getContextPath())
																	.path(relative)
																	// .queryParam("mac", mac)
																	.toUriString();
		}
	}

	@Autowired
	private PropertiesService propertiesService;

	@Override
	public String loadTemplate(final URI baseUrl, final String templateName, final String mac) throws IOException {

		@Cleanup
		final InputStream profileStream = baseUrl.resolve(templateName).toURL().openStream();
		final StringWriter writer = new StringWriter();
		final Context context = new VelocityContext();
		context.put("mac", mac);
		context.put("props", propertiesService);
		context.put("resolver", new UrlResolver(mac));
		Velocity.evaluate(context, writer, "template-" + templateName, new InputStreamReader(profileStream, Charsets.UTF_8));
		writer.flush();
		return writer.toString();
	}

}
