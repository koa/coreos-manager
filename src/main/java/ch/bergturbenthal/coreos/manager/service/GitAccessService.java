package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectLoader;

public interface GitAccessService {

	Map<String, ObjectLoader> findFiles(Collection<String> filenames) throws IOException;

	long getLastModified();
}
