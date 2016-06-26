package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.service.GitAccessService;
import lombok.Cleanup;
import lombok.Getter;

@Service
public class DefaultGitAccess implements GitAccessService {
	private final Git git;
	private long lastFetch;
	@Getter
	private long lastModified;

	public DefaultGitAccess(final Configuration config) throws IOException, InvalidRemoteException, TransportException, GitAPIException {

		final File repositoryTempDir = File.createTempFile("config", ".git");
		repositoryTempDir.delete();
		final CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setBare(true);
		cloneCommand.setURI(config.getRepository());
		cloneCommand.setBranch(config.getBranch());
		cloneCommand.setDirectory(repositoryTempDir);
		git = cloneCommand.call();
		lastFetch = System.currentTimeMillis();
		lastModified = lastFetch;
	}

	@PreDestroy
	public void cleanupTempRepository() throws IOException {
		FileUtils.deleteDirectory(git.getRepository().getDirectory());
	}

	private void fetchIfNeeded() throws IOException {
		try {
			final long now = System.currentTimeMillis();
			if (now - lastFetch > TimeUnit.SECONDS.toMillis(10)) {
				final FetchResult fetchResult = git.fetch().call();
				if (!fetchResult.getTrackingRefUpdates().isEmpty()) {
					lastModified = now;
				}
				lastFetch = now;
			}
		} catch (final GitAPIException e) {
			throw new IOException("cannot fetch", e);
		}

	}

	@Override
	public Map<String, ObjectLoader> findFiles(final Collection<String> filenames) throws IOException {
		fetchIfNeeded();
		final Repository repository = git.getRepository();
		final ObjectId headRef = repository.resolve("HEAD");

		@Cleanup
		final RevWalk revWalk = new RevWalk(repository);
		@Cleanup
		final TreeWalk treeWalk = new TreeWalk(repository);

		final RevCommit headCommit = revWalk.parseCommit(headRef);
		final RevTree tree = headCommit.getTree();
		treeWalk.addTree(tree);

		final Map<String, ObjectLoader> ret = new HashMap<>();
		while (treeWalk.next()) {
			if (treeWalk.isSubtree()) {
				treeWalk.enterSubtree();
				continue;
			}
			final String pathString = treeWalk.getPathString();
			if (!filenames.contains(pathString)) {
				continue;
			}
			final ObjectId objectId = treeWalk.getObjectId(0);
			final ObjectLoader objectLoader = repository.open(objectId, Constants.OBJ_BLOB);
			ret.put(pathString, objectLoader);
		}
		return ret;
	}
}
