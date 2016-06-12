package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
		return "#!ipxe\nchain ipxe?uuid=${uuid}&mac=${net0/mac:hexhyp}&domain=${domain}&hostname=${hostname}&serial=${serial}";
	}

	@RequestMapping(path = "ipxe", produces = "text/plain")
	public String bootWithParams(final HttpServletRequest request) {
		final String requestURL = URI.create(request.getRequestURL().toString()).resolve(".").toString();
		final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
		parameterMap.put("baseUrl", new String[] { requestURL });
		try {
			return configurationService.generatePXE(parameterMap);
		} catch (final Exception ex) {
			log.error("Cannot process data for " + parameterMap, ex);
			return "Error";
		}
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
