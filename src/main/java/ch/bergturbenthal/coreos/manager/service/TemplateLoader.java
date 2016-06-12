package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public interface TemplateLoader {
	String loadTemplate(final URL url, final String templateName, final Map<String, String[]> properties) throws IOException;
}
