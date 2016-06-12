package ch.bergturbenthal.coreos.manager;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import ch.bergturbenthal.coreos.manager.config.Configuration;

public class TestConfiguration {
	@Bean
	public Configuration configuration() throws IOException {
		final Configuration config = new Configuration();
		config.setProfileBase(new ClassPathResource("profiles/default.yml").getURI().resolve("."));
		config.setMacPropertiesConfiguration(new ClassPathResource("csv/test.csv").getURI());
		return config;
	}
}
