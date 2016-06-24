package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController

public class IgnitionController {
	@Autowired
	private ConfigurationService configurationService;

	@RequestMapping(path = "ignition/{filename:.+}", produces = "text/plain")
	public String ignition(@PathVariable("filename") final String filename, @RequestParam("mac") final String mac) throws IOException {
		final String ignitionFile = configurationService.generateIgnition(filename, mac);
		log.info("generated ignition: " + ignitionFile);
		return ignitionFile;
	}

}
