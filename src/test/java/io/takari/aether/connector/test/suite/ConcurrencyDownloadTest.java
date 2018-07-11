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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.util.ChecksumUtils;

import com.google.common.io.Files;

public class ConcurrencyDownloadTest extends AetherTestCase {
    private static final File TMP = new File(System.getProperty("java.io.tmpdir"), "aether-" + UUID.randomUUID().toString().substring(0, 8));

    public void testResumingDownloadsWhereTheClientDiesAndRestarts() throws Exception {
        addDelivery("gid/aid/version/aid-version-classifier.extension", bytesFromFile("test.pom"));
        addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", bytesFromFile("test.pom.sha1"));

        File f = new File(TMP, "foo-bar-1.0.pom");
        Artifact a = artifact("bla");
        ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
        RepositoryConnector connector = connector();

        for (int i = 0; i < 10; ++i) {
            clearRepo();
            doConcurrentCalls(connector, downs);
            assertEquals("12222cd2aa110f969ea1ce71d4595219d2ae6a21", ChecksumUtils.calc(f, Collections.singleton("SHA-1")).get("SHA-1"));
        }
    }

    public void clearRepo() {
        if (TMP.listFiles() != null) {
            for (File file : TMP.listFiles()) {
                file.delete();
            }
        }
    }

    private void doConcurrentCalls(RepositoryConnector c, Collection<? extends ArtifactDownload> downs) throws InterruptedException {
        // Create threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            threads.add(new Thread(new Runnable() {

                @Override
                public void run() {
                    download(c, downs);
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
            Thread.sleep(new Random().nextInt(500));
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
