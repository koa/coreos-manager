package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.util.Map;

import ch.bergturbenthal.coreos.manager.profile.ProfileResult;

public interface ProfileResolver {
	ProfileResult resolveProfile(Map<String, String[]> properties) throws IOException;

	ProfileResult resolveProfile(String profileName, Map<String, String[]> properties) throws IOException;
}
