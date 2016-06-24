package ch.bergturbenthal.coreos.manager.config;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import lombok.Data;

@Data
@ConfigurationProperties("coreos.boot")
@org.springframework.context.annotation.Configuration
public class Configuration {
	private URI bootfileBase;
	private File cacheDir;
	private URI ignitionBase;
	private Resource macPropertiesConfiguration;
	private URI profileBase;
	private Map<String, List<String>> properties;
}
