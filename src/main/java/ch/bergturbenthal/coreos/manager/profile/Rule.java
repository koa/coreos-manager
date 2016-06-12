package ch.bergturbenthal.coreos.manager.profile;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Rule {
	private int priority = 0;
	private Map<String, String[]> properties;
	private List<Match> match;

	public boolean matches(final Map<String, String[]> props) {
		if (match == null) {
			return true;
		}
		return match.stream().allMatch(r -> r.matches(props));
	}

}
