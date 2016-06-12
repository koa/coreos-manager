package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;

import org.springframework.core.io.FileSystemResource;

public interface AssetService {
	FileSystemResource getInitRd(String channel) throws IOException;

	FileSystemResource getKernel(String channel) throws IOException;
}
