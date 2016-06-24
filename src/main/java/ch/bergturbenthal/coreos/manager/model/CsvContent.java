package ch.bergturbenthal.coreos.manager.model;

import java.util.List;
import java.util.Map;

import lombok.Value;

@Value
public class CsvContent {
	private List<String[]> dataLines;
	private Map<String, Integer> headerIndex;
	private String[] headerLine;
}