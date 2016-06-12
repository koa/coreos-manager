package ch.bergturbenthal.coreos.manager.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class Utils {
	public static Map<String, String[]> extractParams(final HttpServletRequest request) {
		final String requestURL = URI.create(request.getRequestURL().toString()).resolve(".").toString();
		final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
		parameterMap.put("baseUrl", new String[] { requestURL });
		return parameterMap;
	}

}
