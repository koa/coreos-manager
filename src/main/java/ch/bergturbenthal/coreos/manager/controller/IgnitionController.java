package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController

public class IgnitionController {
	@Autowired
	private ConfigurationService configurationService;

	@RequestMapping(path = "ignition", produces = "text/plain")
	public String ignition(final HttpServletRequest request) throws IOException {
		log.info("Parameters: "
							+ request.getParameterMap().entrySet().stream().map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue())).collect(Collectors.joining(",")));
		final Map<String, String[]> parameterMap = Utils.extractParams(request);
		final String ignitionFile = configurationService.generateIgnition(parameterMap);
		log.info("generated ignition: " + ignitionFile);
		return ignitionFile;
	}

}
