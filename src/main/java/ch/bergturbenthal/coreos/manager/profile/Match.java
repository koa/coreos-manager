package ch.bergturbenthal.coreos.manager.profile;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class Match {
	public static enum MatchType {
		EXAKT, PREFIX, REGEX
	}

	private String key;
	private MatchType type = MatchType.EXAKT;
	private String value;

	public boolean matches(final Map<String, String[]> parameters) {
		final String[] parameterValues = parameters.get(key);
		if (parameterValues == null) {
			return false;
		}
		final Predicate<String> predicate;
		switch (type) {
		case PREFIX:
			predicate = v -> v.startsWith(value);
			break;
		case REGEX:
			predicate = Pattern.compile(value).asPredicate();
			break;
		default:
			predicate = v -> value.equals(v);
			break;
		}
		return Stream.of(parameterValues).anyMatch(predicate);
	}
}
