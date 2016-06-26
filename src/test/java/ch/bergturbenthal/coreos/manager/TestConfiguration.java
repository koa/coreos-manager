package ch.bergturbenthal.coreos.manager;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import ch.bergturbenthal.coreos.manager.config.Configuration;

public class TestConfiguration {

	private final File tempRepoDir;

	public TestConfiguration() throws IOException, IllegalStateException, GitAPIException {
		tempRepoDir = File.createTempFile("config", "");
		tempRepoDir.delete();
		final Git git = Git.init().setDirectory(tempRepoDir).call();

		final URI profilesBase = new ClassPathResource("config/pxe/default.pxe").getURI().resolve("..");
		final File configDir = new File(profilesBase.getPath());
		FileUtils.copyDirectory(configDir, git.getRepository().getWorkTree());

		git.add().addFilepattern(".").call();

		git.commit().setAll(true).setAuthor("Junit", "test@junit.org").setMessage("imported test config").call();
	}

	@PreDestroy
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(tempRepoDir);
	}

	@Bean
	public Configuration configuration() throws IOException, IllegalStateException, GitAPIException {

		final Configuration config = new Configuration();
		config.setRepository(tempRepoDir.getAbsolutePath());
		config.setBranch("master");
		config.setMacPropertiesConfiguration(new ClassPathResource("csv/test.csv"));
		return config;
	}
}
