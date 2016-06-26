package ch.bergturbenthal.coreos.manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Import(TestConfiguration.class)
public class CoreosManagerApplicationTests {

	@Autowired
	private ConfigurationService configurationService;

	@Test
	public void contextLoads() {
	}

	private Map<String, String[]> defaultParameterMap() {
		final Map<String, String[]> ret = new HashMap<String, String[]>();
		ret.put("mac", new String[] { "00-0d-b9-35-b7-9c" });
		ret.put("baseUrl", new String[] { "http://localhost:8080/" });
		return ret;
	}

	@Test
	public void testIgnition() throws IOException {
		final Resource ignition = configurationService.generateFile("ignition/install-reboot.json", "00-0d-b9-35-b7-9c");
		for (final String line : IOUtils.readLines(ignition.getInputStream())) {
			log.info("- Ignition -" + line);

		}
	}

	@Test
	public void testIgnitionK8sMaster() throws IOException {
		for (final String line : IOUtils.readLines(configurationService.generateFile("ignition/k8s-master.yml", "00-0d-b9-35-b7-9c").getInputStream())) {
			log.info("- i-k8s-m - " + line);

		}
		for (final String line : IOUtils.readLines(configurationService.generateFile("ignition/files/options.env", "00-0d-b9-35-b7-9c").getInputStream())) {
			log.info("- e-k8s-m - " + line);

		}
	}

	@Test
	public void testPxe() throws IOException {
		final Resource pxe = configurationService.generatePXE("00-0d-b9-35-b7-9c");
		for (final String line : IOUtils.readLines(pxe.getInputStream())) {
			log.info("- PXE -" + line);

		}

	}

	@Test
	public void testPxe2() throws IOException {
		final Resource pxe = configurationService.generatePXE("123");
		for (final String line : IOUtils.readLines(pxe.getInputStream())) {
			log.info("- PXE2 -" + line);

		}
	}

}
