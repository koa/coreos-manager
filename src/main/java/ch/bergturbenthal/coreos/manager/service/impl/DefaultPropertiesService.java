package ch.bergturbenthal.coreos.manager.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.coreos.manager.model.CsvContent;
import ch.bergturbenthal.coreos.manager.service.CsvLoaderService;
import ch.bergturbenthal.coreos.manager.service.PropertiesService;
import lombok.Value;

@Service
public class DefaultPropertiesService implements PropertiesService {
	@Value
	private static class Hit {
		private CsvContent data;
		private String[] row;
	}

	private final CsvLoaderService loaderService;

	@Autowired
	public DefaultPropertiesService(final CsvLoaderService loaderService) {
		this.loaderService = loaderService;
	}

	@Override
	public Stream<Map<String, String>> allPropertiesOfKey(final String keyColumn, final String keyValue) {
		final Stream<Hit> hitStream = filterHit(keyColumn, keyValue);

		return hitStream.map(t -> {
			final String[] dataLine = t.getRow();
			final String[] headerLine = t.getData().getHeaderLine();
			final int columnCount = Math.min(dataLine.length, headerLine.length);
			final Map<String, String> rowMap = new LinkedHashMap<String, String>();
			for (int i = 0; i < columnCount; i++) {
				rowMap.put(headerLine[i], dataLine[i]);
			}
			return rowMap;
		});
	}

	private Stream<Hit> filterHit(final String keyColumn, final String keyValue) {
		final CsvContent data = loaderService.loadData();

		final Integer keyColumnIndex = data.getHeaderIndex().get(keyColumn);
		if (keyColumnIndex == null) {
			return Stream.empty();
		}
		final int index = keyColumnIndex.intValue();
		return data.getDataLines().stream().map(new Function<String[], Hit>() {

			@Override
			public Hit apply(final String[] dataLine) {
				if (dataLine.length <= index) {
					return null;
				}
				final String rowKeyValue = dataLine[index];
				if (rowKeyValue == null || !rowKeyValue.equals(keyValue)) {
					return null;
				}
				return new Hit(data, dataLine);
			}
		}).filter(t -> t != null);
	}

	@Override
	public Stream<String> valuesOfKey(final String keyColumn, final String keyValue, final String resultColumn) {
		return filterHit(keyColumn, keyValue).map(new Function<DefaultPropertiesService.Hit, String>() {

			@Override
			public String apply(final Hit t) {
				final Integer resultIndex = t.getData().getHeaderIndex().get(resultColumn);
				if (resultIndex == null) {
					return null;
				}
				final String[] row = t.getRow();
				if (row.length <= resultIndex.intValue()) {
					return null;
				}
				return row[resultIndex.intValue()];

			}
		}).filter(t -> t != null);
	}
}
