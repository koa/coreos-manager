package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.profile.ProfileMeta;
import ch.bergturbenthal.coreos.manager.profile.ProfileResult;
import ch.bergturbenthal.coreos.manager.profile.Rule;
import ch.bergturbenthal.coreos.manager.service.ProfileResolver;
import ch.bergturbenthal.coreos.manager.service.YamlLoader;
import lombok.Cleanup;

@Service
public class DefaultProfileResolver implements ProfileResolver {

	private final URI macPropertiesConfiguration;
	private final URI profileBase;
	private final YamlLoader yamlLoader;

	@Autowired
	public DefaultProfileResolver(final Configuration config, final YamlLoader yamlLoader) {
		this.yamlLoader = yamlLoader;
		profileBase = config.getProfileBase();
		macPropertiesConfiguration = config.getMacPropertiesConfiguration();
	}

	private String getProfile(final String[] profileValues) {
		if (profileValues != null) {
			for (final String value : profileValues) {
				if (value.length() > 0) {
					return value;
				}

			}
		}
		return "default";
	}

	@Override
	public ProfileResult resolveProfile(final Map<String, String[]> parameterMap) throws IOException {
		final Map<String, String[]> properties = new HashMap<String, String[]>(parameterMap);
		if (macPropertiesConfiguration != null) {
			@Cleanup
			final CSVReader csvReader = new CSVReader(new InputStreamReader(macPropertiesConfiguration.toURL().openStream(), Charsets.UTF_8));
			final Iterator<String[]> csvIterator = csvReader.iterator();
			if (csvIterator.hasNext()) {
				final String[] header = csvIterator.next();
				while (csvIterator.hasNext()) {
					final String[] dataLine = csvIterator.next();
					final int columnCount = Math.min(header.length, dataLine.length);
					final Map<String, String[]> content = new HashMap<String, String[]>();
					final Map<String, String> keyData = new HashMap<String, String>();
					for (int i = 0; i < columnCount; i++) {
						final String columnName = header[i];
						if (columnName == null) {
							continue;
						}
						final String value = dataLine[i];
						if (value == null) {
							continue;
						}
						if (columnName.startsWith("key.")) {
							keyData.put(columnName.substring(4), value);
						} else {
							content.put(columnName, value.split(";"));
						}
					}
					if (keyData.entrySet().stream().allMatch((Predicate<Entry<String, String>>) e -> {
						final String[] values = properties.get(e.getKey());
						if (values == null) {
							return false;
						}
						return Stream.of(values).anyMatch(v -> v.equals(e.getValue()));
					})) {
						content.forEach((BiConsumer<String, String[]>) (t,
																														u) -> properties.merge(	t,
																																										u,
																																										(BiFunction<String[], String[], ? extends String[]>) (t1,
																																																																					u1) -> ArrayUtils.addAll(	u1,
																																																																																		t1)));
					}
				}
			}
		}
		final String profileName = getProfile(properties.get("profile"));

		return resolveProfile(profileName, properties);
	}

	@Override
	public ProfileResult resolveProfile(final String profileName, final Map<String, String[]> properties) throws IOException {
		final URI profileUri = profileBase.resolve(profileName + ".yml");
		final ProfileMeta profile = yamlLoader.loadYaml(profileUri.toURL(), profileName, properties, ProfileMeta.class);

		final Map<String, String[]> mergedProps = new HashMap<>(properties);
		final List<Rule> rules = profile.getRules();
		if (rules != null) {
			rules	.stream()
						.filter((Predicate<Rule>) t -> t.matches(properties))
						.forEach((Consumer<Rule>) t -> t.getProperties().forEach((BiConsumer<String, String[]>) (propertyName, rulePropertyValue) -> mergedProps.merge(	propertyName,
																																																																														rulePropertyValue,
																																																																														(BiFunction<String[], String[], String[]>) (v1,
																																																																																																				v2) -> ArrayUtils.addAll(	v1,
																																																																																																																	v2))));
		}

		final String parentProfileName = profile.getParentProfile();
		if (parentProfileName != null) {
			final ProfileResult parentProfile = resolveProfile(parentProfileName, mergedProps);
			return parentProfile.mergeChildSpec(profile.getSpec());

		}
		return new ProfileResult(mergedProps, profile.getSpec());
	}

}
