package ch.bergturbenthal.coreos.manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CoreosManagerApplication {

	public static void main(final String[] args) {
		SpringApplication.run(CoreosManagerApplication.class, args);
	}

	@Bean
	public ExecutorService executorService() {
		return Executors.newFixedThreadPool(3);
	}
}
