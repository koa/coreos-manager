package ch.bergturbenthal.coreos.manager.service;

import java.util.Map;
import java.util.stream.Stream;

public interface PropertiesService {
	Stream<Map<String, String>> allPropertiesOfKey(String keyColumn, String keyValue);

	Stream<String> valuesOfKey(String keyColumn, String keyValue, String resultColumn);
}
