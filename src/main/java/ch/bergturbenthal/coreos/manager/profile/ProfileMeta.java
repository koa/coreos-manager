package ch.bergturbenthal.coreos.manager.profile;

import java.util.List;

import lombok.Data;

@Data
public class ProfileMeta {
	private String parentProfile;
	private List<Rule> rules;
	private ProfileSpecification spec;
}
