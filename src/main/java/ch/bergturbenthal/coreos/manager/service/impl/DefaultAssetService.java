package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.service.AssetService;
import ch.bergturbenthal.coreos.manager.service.ProxyService;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultAssetService implements AssetService, ProxyService {
	private class Handle {
		private final AtomicReference<Throwable> exception = new AtomicReference<>();
		private final String hash;
		private final AtomicReference<File> loadedFile = new AtomicReference<File>(null);
		private final Queue<DeferredResult<File>> waitingResults = new ConcurrentLinkedQueue<>();

		public Handle(final String hash, final URI uri) {
			this.hash = hash;
			executorervice.submit(new Runnable() {

				@Override
				public void run() {
					try {
						final File outFile = new File(cacheDir, hash);
						if (outFile.exists()) {
							loadedFile.set(outFile);
							return;
						}
						final File tempFile = File.createTempFile("temp", hash, cacheDir);
						{
							@Cleanup
							final InputStream is = uri.toURL().openStream();
							@Cleanup
							final FileOutputStream os = new FileOutputStream(tempFile);
							IOUtils.copy(is, os);
						}
						tempFile.renameTo(outFile);
						loadedFile.set(outFile);
					} catch (final IOException e) {
						exception.set(e);
						log.error("Canot load " + uri, e);
					} finally {
						pushResult();
					}
				}
			});
		}

		private void pushResult() {
			final File foundFile = loadedFile.get();
			if (foundFile != null) {
				while (true) {
					final DeferredResult<File> pendingResult = waitingResults.poll();
					if (pendingResult == null) {
						break;
					}
					pendingResult.setResult(foundFile);
				}
			}
			final Throwable throwable = exception.get();
			if (throwable != null) {
				while (true) {
					final DeferredResult<File> pendingResult = waitingResults.poll();
					if (pendingResult == null) {
						break;
					}
					pendingResult.setErrorResult(throwable);
				}
			}
		}

		public DeferredResult<File> takeFile() {
			final DeferredResult<File> ret = new DeferredResult<>();
			waitingResults.add(ret);
			pushResult();
			return ret;
		}
	}

	private final File cacheDir;
	private final boolean cleanupCacheDir;

	private final ClientHttpRequestFactory clientHttpRequestFactory;

	private final ExecutorService executorervice;

	private final Map<String, Handle> existingHandles = new ConcurrentHashMap<>();

	@Autowired
	public DefaultAssetService(final Configuration config, final ExecutorService executorService) throws IOException {
		executorervice = executorService;
		this.clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		final File configuredCacheDir = config.getCacheDir();
		if (configuredCacheDir == null) {
			final File tempFile = File.createTempFile("coreos-cache", "");
			tempFile.delete();
			cacheDir = tempFile;
			cleanupCacheDir = true;
		} else {
			cacheDir = configuredCacheDir;
			cleanupCacheDir = false;
		}
		cacheDir.mkdirs();
	}

	@PreDestroy
	public void cleanup() throws IOException {
		if (cleanupCacheDir) {
			FileUtils.deleteDirectory(cacheDir);
		}
	}

	@Override
	@SneakyThrows
	public String createHandle(final URI uri) {
		final String uriString = uri.toString();
		final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		final String hexString = new BigInteger(1, messageDigest.digest(uriString.getBytes(StandardCharsets.UTF_8))).toString(16);
		existingHandles.computeIfAbsent(hexString, (Function<? super String, ? extends Handle>) key -> new Handle(hexString, uri));
		return hexString;
	}

	private File downloadUrl(final String targetFilename, final URL url) {
		try {
			final File cachedFile = new File(cacheDir, targetFilename);
			while (!cachedFile.exists()) {
				try {
					final File tempFile = File.createTempFile("download", "", cacheDir);
					try {
						cachedFile.getParentFile().mkdirs();
						log.info("Download " + url + " -> " + targetFilename);
						{
							final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(url.toURI(), HttpMethod.GET);
							final ClientHttpResponse response = clientHttpRequest.execute();
							@Cleanup
							final InputStream is = response.getBody();
							final ReadableByteChannel rbc = Channels.newChannel(is);
							@Cleanup
							final FileOutputStream os = new FileOutputStream(tempFile);
							os.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						}
						tempFile.renameTo(cachedFile);
					} finally {
						tempFile.delete();
					}
				} catch (final IOException e) {
					log.error("Error downloadind File", e);
				}
			}
			return cachedFile;
		} catch (final URISyntaxException e) {
			throw new RuntimeException("Cannot parse uri", e);
		}
	}

	@Override
	public FileSystemResource getFile(final String channel, final String version, final String filename) throws IOException {
		final String targetFilename = channel + "/" + version + "/" + filename;
		final URL url = new URL("http://" + channel + ".release.core-os.net/amd64-usr/" + version + "/" + filename);
		return new FileSystemResource(downloadUrl(targetFilename, url));

	}

	@Override
	public FileSystemResource getInitRd(final String channel) throws IOException {
		return new FileSystemResource(downloadUrl("initrd-"	+ channel,
																							new URL("http://" + channel + ".release.core-os.net/amd64-usr/current/coreos_production_pxe_image.cpio.gz")));
	}

	@Override
	public FileSystemResource getKernel(final String channel) throws IOException {
		final File downloadedFile = downloadUrl("kernel-" + channel, new URL("http://" + channel + ".release.core-os.net/amd64-usr/current/coreos_production_pxe.vmlinuz"));

		return new FileSystemResource(downloadedFile);
	}

	@Override
	public DeferredResult<FileSystemResource> loadByHandle(final String id) {
		final Handle handle = existingHandles.get(id);
		final DeferredResult<FileSystemResource> ret = new DeferredResult<>();
		final DeferredResult<File> file = handle.takeFile();
		final Runnable callback = () -> {
			final Object result = file.getResult();
			if (result == null) {
				return;
			}
			if (result instanceof File) {
				ret.setResult(new FileSystemResource((File) result));
			} else {
				ret.setErrorResult(result);
			}
		};
		file.onCompletion(callback);
		callback.run();
		return ret;
	}

}
