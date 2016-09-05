package ch.bergturbenthal.coreos.manager.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PxeController {

	@Autowired
	private ConfigurationService configurationService;

	@RequestMapping(path = { "boot.ipxe", "boot.ipxe.0" }, produces = "text/plain")
	public String boot() {
		return "#!ipxe\nchain ipxe?mac=${net0/mac:hexhyp}&uuid=${uuid}";
	}

	@RequestMapping(path = "ipxe", produces = "text/plain")
	public Resource bootWithParams(@RequestParam("mac") final String mac, @RequestParam("uuid") final String uuid) {
		try {
			log.info("pxe boot " + mac + ", " + uuid);
			return configurationService.generatePXE(mac);
		} catch (final Exception ex) {
			log.error("Cannot process data for " + mac, ex);
			return new ByteArrayResource("Error".getBytes(StandardCharsets.UTF_8));
		}
	}

}
