package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;

import org.springframework.core.io.Resource;

public interface ConfigurationService {

	Resource generateFile(String relativePath, String mac) throws IOException;

	Resource generatePXE(String mac) throws IOException;

}
