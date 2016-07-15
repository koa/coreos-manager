package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.service.AssetService;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultAssetService implements AssetService {
	private final File cacheDir;
	private final boolean cleanupCacheDir;
	private final ClientHttpRequestFactory clientHttpRequestFactory;

	@Autowired
	public DefaultAssetService(final Configuration config) throws IOException {
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
		return new FileSystemResource(downloadUrl("initrd-"+ channel,
																							new URL("http://" + channel + ".release.core-os.net/amd64-usr/current/coreos_production_pxe_image.cpio.gz")));
	}

	@Override
	public FileSystemResource getKernel(final String channel) throws IOException {
		final File downloadedFile = downloadUrl("kernel-" + channel, new URL("http://" + channel + ".release.core-os.net/amd64-usr/current/coreos_production_pxe.vmlinuz"));

		return new FileSystemResource(downloadedFile);
	}

}
