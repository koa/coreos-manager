package ch.bergturbenthal.coreos.manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.service.ProfileResolver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Import(TestConfiguration.class)
public class CoreosManagerApplicationTests {

	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private ProfileResolver resolver;

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
	public void testPxe() throws IOException {
		final String pxe = configurationService.generatePXE(defaultParameterMap());
		log.info("------ PXE ------\n" + pxe);
	}

	@Test
	public void testPxe2() throws IOException {
		final Map<String, String[]> parameterMap = defaultParameterMap();
		parameterMap.put("mac", new String[] { "123" });
		final String pxe = configurationService.generatePXE(parameterMap);
		log.info("------ PXE2 ------\n" + pxe);
	}

	@Test
	public void testResolver() throws IOException {
		Assert.assertNotNull(resolver.resolveProfile(defaultParameterMap()));
	}

}
