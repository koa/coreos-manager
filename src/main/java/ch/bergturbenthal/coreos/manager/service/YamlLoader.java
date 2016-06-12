package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public interface YamlLoader {
	<T> T loadYaml(final URL url, final String templateName, final Map<String, String[]> properties, final Class<T> resultType) throws IOException;
}
