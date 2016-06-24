package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;

public interface ConfigurationService {

	String generateIgnition(String filename, String mac) throws IOException;

	String generatePXE(String filename) throws IOException;

}
