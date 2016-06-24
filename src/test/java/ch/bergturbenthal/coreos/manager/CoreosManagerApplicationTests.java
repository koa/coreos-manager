package ch.bergturbenthal.coreos.manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
		final String ignition = configurationService.generateIgnition("default", "00-0d-b9-35-b7-9c");
		log.info("------ Ignition ------\n" + ignition);

	}

	@Test
	public void testPxe() throws IOException {
		final String pxe = configurationService.generatePXE("00-0d-b9-35-b7-9c");
		log.info("------ PXE ------\n" + pxe);
	}

	@Test
	public void testPxe2() throws IOException {
		final String pxe = configurationService.generatePXE("123");
		log.info("------ PXE2 ------\n" + pxe);
	}

}
