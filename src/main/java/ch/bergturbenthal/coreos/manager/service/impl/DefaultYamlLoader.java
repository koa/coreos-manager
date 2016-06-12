package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import ch.bergturbenthal.coreos.manager.service.TemplateLoader;
import ch.bergturbenthal.coreos.manager.service.YamlLoader;

@Service
public class DefaultYamlLoader implements YamlLoader {
	@Autowired
	private TemplateLoader templateLoader;

	private final Yaml yaml = new Yaml();

	public DefaultYamlLoader() {

	}

	@Override
	public <T> T loadYaml(final URL url, final String templateName, final Map<String, String[]> properties, final Class<T> resultType) throws IOException {
		final String templateString = templateLoader.loadTemplate(url, templateName, properties);
		try {
			return yaml.loadAs(new StringReader(templateString), resultType);
		} catch (final YAMLException ex) {
			throw new RuntimeException("Cannot parse " + templateString, ex);
		}
	}

}
