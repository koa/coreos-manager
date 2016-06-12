package ch.bergturbenthal.coreos.manager.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSpecification {
	private String channel;
	private String ignitionTemplate;
	private Map<String, String[]> kernelParameters;

	private Map<String, String[]> mergeKernelParams(final Map<String, String[]> params, final Map<String, String[]> parentParams) {
		if (params == null) {
			return parentParams;
		}
		if (parentParams == null) {
			return params;
		}
		final Map<String, List<String>> mergedValues = new HashMap<>();
		for (final Entry<String, String[]> paramEntry : parentParams.entrySet()) {
			mergedValues.put(paramEntry.getKey(), new ArrayList<String>(Arrays.asList(paramEntry.getValue())));
		}
		for (final Entry<String, String[]> paramEntry : params.entrySet()) {
			mergedValues.computeIfAbsent(paramEntry.getKey(), k -> new ArrayList<>()).addAll(Arrays.asList(paramEntry.getValue()));

		}
		final Map<String, String[]> ret = new HashMap<String, String[]>();
		for (final Entry<String, List<String>> mergedEntry : mergedValues.entrySet()) {
			final List<String> value = mergedEntry.getValue();
			ret.put(mergedEntry.getKey(), value.toArray(new String[value.size()]));
		}
		return ret;
	}

	public ProfileSpecification mergeParent(final ProfileSpecification parentSpec) {
		return new ProfileSpecification(mergeString(channel, parentSpec.getChannel()),
																		mergeString(ignitionTemplate, parentSpec.getChannel()),
																		mergeKernelParams(kernelParameters, parentSpec.getKernelParameters()));
	}

	private String mergeString(final String value, final String parentValue) {
		if (value == null) {
			return parentValue;
		} else {
			return value;
		}
	}
}
