package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.model.CsvContent;
import ch.bergturbenthal.coreos.manager.service.CsvLoaderService;
import lombok.Cleanup;

@Service
public class DefaultCsvLoader implements CsvLoaderService {
	private final Resource macPropertiesConfiguration;

	@Autowired
	public DefaultCsvLoader(final Configuration configuration) {
		macPropertiesConfiguration = configuration.getMacPropertiesConfiguration();
	}

	@Override
	@Cacheable
	public CsvContent loadData() {
		try {
			@Cleanup
			final InputStream inputStream = macPropertiesConfiguration.getInputStream();
			@Cleanup
			final CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, Charsets.UTF_8));
			final Iterator<String[]> csvIterator = csvReader.iterator();
			final Map<String, Integer> headerIndex = new HashMap<String, Integer>();
			final List<String[]> dataLines = new ArrayList<>();
			final String[] header;
			if (csvIterator.hasNext()) {
				header = csvIterator.next();
				for (int i = 0; i < header.length; i++) {
					headerIndex.put(header[i], i);
				}
				while (csvIterator.hasNext()) {
					final String[] dataLine = csvIterator.next();
					dataLines.add(dataLine);
				}
			} else {
				header = new String[] {};
			}
			return new CsvContent(dataLines, headerIndex, header);
		} catch (final IOException e) {
			throw new RuntimeException("Cannot load properties", e);
		}
	}
}
