package ch.bergturbenthal.coreos.manager;

import java.io.IOException;
import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import ch.bergturbenthal.coreos.manager.config.Configuration;

public class TestConfiguration {
	@Bean
	public Configuration configuration() throws IOException {
		final Configuration config = new Configuration();
		final URI profilesBase = new ClassPathResource("profiles/default.yml").getURI().resolve(".");
		config.setProfileBase(profilesBase);
		config.setIgnitionBase(profilesBase.resolve("../ignition/"));
		config.setBootfileBase(profilesBase.resolve("../pxe/"));
		config.setMacPropertiesConfiguration(new ClassPathResource("csv/test.csv"));
		return config;
	}
}
