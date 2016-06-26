package ch.bergturbenthal.coreos.manager.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import lombok.Data;

@Data
@ConfigurationProperties("coreos.boot")
@org.springframework.context.annotation.Configuration
public class Configuration {
	private String branch;
	private File cacheDir;
	private Resource macPropertiesConfiguration;
	private Map<String, List<String>> properties;
	private String repository;
}
