package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.net.URI;

public interface TemplateLoader {
	String loadTemplate(final URI ignitionBase, final String templateName, String mac) throws IOException;
}
