package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.coreos.manager.service.AssetService;
import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.util.Utils;
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
		log.info("Parameters: "
							+ request.getParameterMap().entrySet().stream().map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue())).collect(Collectors.joining(",")));
		final Map<String, String[]> parameterMap = Utils.extractParams(request);
		try {
			return configurationService.generatePXE(parameterMap);
		} catch (final Exception ex) {
			log.error("Cannot process data for " + parameterMap, ex);
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
