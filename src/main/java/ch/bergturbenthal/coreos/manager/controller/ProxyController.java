package ch.bergturbenthal.coreos.manager.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import ch.bergturbenthal.coreos.manager.service.AssetService;
import ch.bergturbenthal.coreos.manager.service.ProxyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ProxyController {
	@Autowired
	private AssetService assetService;
	@Autowired
	private ProxyService proxyService;

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

	@RequestMapping("genproxy/{handle}")
	public DeferredResult<FileSystemResource> proxyFile(@PathVariable("handle") final String handle) {
		return proxyService.loadByHandle(handle);
	}

}
