package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.util.Map;

public interface ConfigurationService {

	String generatePXE(Map<String, String[]> parameterMap) throws IOException;

}
