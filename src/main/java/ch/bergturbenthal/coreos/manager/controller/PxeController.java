package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.coreos.manager.service.AssetService;
import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PxeController {

	@Autowired
	private AssetService assetService;
	@Autowired
	private ConfigurationService configurationService;

	@RequestMapping(path = { "boot.ipxe", "boot.ipxe.0" }, produces = "text/plain")
	public String boot() {
		return "#!ipxe\nchain ipxe?uuid=${uuid}&mac=${net0/mac:hexhyp}";
	}

	@RequestMapping(path = "ipxe", produces = "text/plain")
	public String bootWithParams(@RequestParam("mac") final String mac) {
		try {
			return configurationService.generatePXE(mac);
		} catch (final Exception ex) {
			log.error("Cannot process data for " + mac, ex);
			return "Error";
		}
	}

	@RequestMapping("proxy/{channel}/{version}/{filename:.+}")
	public FileSystemResource loadFile(	@PathVariable("channel") final String channel,
																			@PathVariable("version") final String version,
																			@PathVariable("filename") final String filename) throws IOException {
		return assetService.getFile(channel, version, filename);
	}

	@RequestMapping("initrd/{channel}")
	public FileSystemResource loadInitRd(@PathVariable("channel") final String channel) throws IOException {
		return assetService.getInitRd(channel);
	}

	@RequestMapping("kernel/{channel}")
	public FileSystemResource loadKernel(@PathVariable("channel") final String channel) throws IOException {
		return assetService.getKernel(channel);
	}
}
