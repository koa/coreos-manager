package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.service.PropertiesService;
import ch.bergturbenthal.coreos.manager.service.TemplateLoader;

@Service
public class DefaultConfigurationService implements ConfigurationService {

	private final URI bootfileBase;
	private final URI ignitionBase;
	private final Map<String, List<String>> properties;
	private final PropertiesService propertiesService;
	private final TemplateLoader templateLoader;

	@Autowired
	public DefaultConfigurationService(final PropertiesService propertiesService, final Configuration configuration, final TemplateLoader templateLoader) {
		this.propertiesService = propertiesService;
		this.templateLoader = templateLoader;
		ignitionBase = configuration.getIgnitionBase();
		properties = configuration.getProperties();
		bootfileBase = configuration.getBootfileBase();
	}

	@Override
	public String generateIgnition(final String file, final String mac) throws IOException {
		final String yamlTemplate = templateLoader.loadTemplate(ignitionBase, file, mac);
		final Yaml yaml = new Yaml();
		final Map<String, Object> data = (Map<String, Object>) yaml.load(yamlTemplate);
		final JSONObject jsonObject = new JSONObject(data);
		return jsonObject.toString();
	}

	@Override
	public String generatePXE(final String mac) throws IOException {
		final String pxeFile = propertiesService.valuesOfKey("mac", mac, "pxe").findFirst().orElse("default") + ".pxe";
		return templateLoader.loadTemplate(bootfileBase, pxeFile, mac);

	}

}
