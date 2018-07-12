/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.aether.connector.test.suite;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.util.ChecksumUtils;

import com.google.inject.Binder;

public class ConcurrentDownloadTest extends AetherTestCase {
	private static final String EXPECTED_CHECKSUM = "7f8527f31177c8c1c51fa356c8da9c4a0ee82787";
	private static final File TMP = new File(System.getProperty("java.io.tmpdir"), "aether-" + UUID.randomUUID().toString().substring(0, 8));

	@Override
	public void configure(Binder binder) {
		binder.bind(FileProcessor.class).to(DefaultFileProcessor.class);
		super.configure(binder);
	}

	public void testResumingDownloadsWhereTheClientDiesAndRestarts() throws Exception {
		addDelivery("gid/aid/version/aid-version-classifier.extension", bytesFromFile("test.pom"));
		addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", EXPECTED_CHECKSUM.getBytes());

		File f = new File(TMP, "foo-bar-1.0.pom");
		Artifact artifact = artifact("bla");
		ArtifactDownload down = new ArtifactDownload(artifact, null, f, RepositoryPolicy.CHECKSUM_POLICY_WARN);
		Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
		RepositoryConnector connector = connector();

		for (int i = 0; i < 10; ++i) {
			clearRepo();
			doConcurrentCalls(connector, downs);
			assertEquals(EXPECTED_CHECKSUM, ChecksumUtils.calc(f, Collections.singleton("SHA-1")).get("SHA-1"));
		}
	}

	public void clearRepo() {
		if (TMP.listFiles() != null) {
			for (File file : TMP.listFiles()) {
				file.delete();
			}
		}
	}

	private void doConcurrentCalls(final RepositoryConnector connector,
			final Collection<? extends ArtifactDownload> downs) throws InterruptedException {
		// Create threads
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < 2; ++i) {
			threads.add(new Thread(new Runnable() {
				@Override
				public void run() {
					download(connector, downs);
				}
			}));
		}

		// Start threads
		for (Thread thread : threads) {
			thread.start();
		}

		// Wait for them to finish
		for (Thread thread : threads) {
			thread.join();
		}

	}

	private void download(RepositoryConnector connector, Collection<? extends ArtifactDownload> downs) {
		try {
			Thread.sleep(new Random().nextInt(100));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connector.get(downs, new ArrayList<MetadataDownload>());
	}

	private static byte[] bytesFromFile(String string) throws URISyntaxException, IOException {
		Path uri = Paths.get(AetherConnectorTest.class.getResource("/" + string).toURI());
		return java.nio.file.Files.readAllBytes(uri);
	}

}
