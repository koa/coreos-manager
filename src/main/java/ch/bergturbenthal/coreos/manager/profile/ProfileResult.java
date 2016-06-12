package ch.bergturbenthal.coreos.manager.profile;

import java.util.Map;

import lombok.Value;

@Value
public class ProfileResult {
	private Map<String, String[]> properties;
	private ProfileSpecification specification;

	public ProfileResult mergeChildSpec(final ProfileSpecification childSpec) {
		return new ProfileResult(properties, mergeSpec(childSpec, specification));
	}

	private ProfileSpecification mergeSpec(final ProfileSpecification spec, final ProfileSpecification parentSpec) {
		if (spec == null) {
			return parentSpec;
		}
		if (parentSpec == null) {
			return spec;
		}

		return spec.mergeParent(parentSpec);
	}
}
