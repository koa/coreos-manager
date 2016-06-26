package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import lombok.SneakyThrows;

public class DelegatingResourceLoader extends ResourceLoader {
	private static ThreadLocal<ResourceLoader> currentResourceloader = new ThreadLocal<ResourceLoader>();

	@SneakyThrows
	public static <T> T callWithLoader(final Callable<T> callable, final ResourceLoader loader) {
		final ResourceLoader loaderOutside = currentResourceloader.get();
		currentResourceloader.set(loader);
		try {
			return callable.call();
		} finally {
			currentResourceloader.set(loaderOutside);
		}
	}

	@Override
	public long getLastModified(final Resource resource) {
		return currentResourceloader.get().getLastModified(resource);
	}

	@Override
	public InputStream getResourceStream(final String source) throws ResourceNotFoundException {
		final ResourceLoader resourceLoader = currentResourceloader.get();
		if (resourceLoader == null) {
			throw new ResourceNotFoundException("no current resource loader");
		}
		return resourceLoader.getResourceStream(source);
	}

	@Override
	public void init(final ExtendedProperties configuration) {
		// configure otherwise
	}

	@Override
	public boolean isSourceModified(final Resource resource) {
		return currentResourceloader.get().isSourceModified(resource);
	}
}
