package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.coreos.manager.profile.ProfileResult;
import ch.bergturbenthal.coreos.manager.profile.ProfileSpecification;
import ch.bergturbenthal.coreos.manager.service.ConfigurationService;
import ch.bergturbenthal.coreos.manager.service.ProfileResolver;

@Service
public class DefaultConfigurationService implements ConfigurationService {

	private final ProfileResolver profileResolver;

	@Autowired
	public DefaultConfigurationService(final ProfileResolver profileResolver) {
		this.profileResolver = profileResolver;
	}

	@Override
	public String generatePXE(final Map<String, String[]> parameterMap) throws IOException {
		final ProfileResult resolvedProfile = profileResolver.resolveProfile(parameterMap);
		final ProfileSpecification spec = resolvedProfile.getSpecification();
		final String channel = resolvedProfile.getSpecification().getChannel();
		final StringBuilder output = new StringBuilder("#!ipxe\nkernel /kernel/");
		output.append(channel);
		final Map<String, String[]> kernelParameters = spec.getKernelParameters();

		if (kernelParameters != null) {
			kernelParameters.forEach((key, valueList) -> {
				if (valueList == null) {
					output.append(" ");
					output.append(key);
				} else {
					Stream.of(valueList).forEach(v -> {
						output.append(" ");
						output.append(key);
						if (v != null && !v.isEmpty()) {
							output.append("=");
							output.append(v);
						}
					});
				}
			});
		}

		output.append("\ninitrd /initrd/");
		output.append(channel);
		output.append("\nboot\n");
		return output.toString();
	}

}
