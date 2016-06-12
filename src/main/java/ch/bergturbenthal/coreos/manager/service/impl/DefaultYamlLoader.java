package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import ch.bergturbenthal.coreos.manager.service.YamlLoader;
import lombok.Cleanup;

@Service
public class DefaultYamlLoader implements YamlLoader {
	private final Yaml yaml = new Yaml();

	public DefaultYamlLoader() {

	}

	@Override
	public <T> T loadYaml(final URL url, final String templateName, final Map<String, String[]> properties, final Class<T> resultType) throws IOException {
		@Cleanup
		final InputStream profileStream = url.openStream();
		final StringWriter writer = new StringWriter();
		final Map<String, List<String>> params = new HashMap<String, List<String>>();
		properties.forEach((key, value) -> params.put(key, Arrays.asList(value)));
		final Context context = new VelocityContext(params);
		Velocity.evaluate(context, writer, "template-" + templateName, new InputStreamReader(profileStream, Charsets.UTF_8));
		writer.flush();
		try {
			return yaml.loadAs(new StringReader(writer.toString()), resultType);
		} catch (final YAMLException ex) {
			throw new RuntimeException("Cannot parse " + writer.toString(), ex);
		}
	}

}
