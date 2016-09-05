package ch.bergturbenthal.coreos.manager.service;

import java.net.URI;

import org.springframework.core.io.FileSystemResource;
import org.springframework.web.context.request.async.DeferredResult;

public interface ProxyService {
	String createHandle(URI uri);

	DeferredResult<FileSystemResource> loadByHandle(String handle);
}
