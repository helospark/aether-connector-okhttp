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
import java.util.Arrays;

import javax.inject.Inject;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.SimpleLoggerFactory;

import com.google.inject.Binder;

public class GetRetryTest extends InjectedTestCase {

  @Inject
  private RepositoryConnectorFactory factory;

  @Inject
  private DefaultRepositorySystemSession session;

  private Artifact artifact;

  private Server server;
  private Connector connector;
  private FlakyHandler flakyHandler;

  // NOTE: Length of pattern should not be divisable by 2 to catch data continuation errors during resume
  static final int[] CONTENT_PATTERN = {
      'a', 'B', ' '
  };

  public void tearDown() throws Exception {
    
    if (server != null) {
      server.stop();
    }

    factory = null;
    session = null;
    server = null;

    TestFileUtils.deleteTempFiles();
  }

  public String url() {
    return "http://localhost:" + connector.getLocalPort() + "/";
  }

  private void assertContentPattern(File file) throws IOException {
    byte[] content = TestFileUtils.readBytes(file);
    for (int i = 0; i < content.length; i++) {
      assertEquals(file.getAbsolutePath() + " corrupted at offset " + i, CONTENT_PATTERN[i % CONTENT_PATTERN.length], content[i]);
    }
  }

  public void testDownloadWhenServerCannotSatisfyDownloadWithoutRangesWillResultInException() throws Exception {
    
    artifact = new DefaultArtifact("gid", "aid", "classifier", "extension", "version");

    server = new Server();    
    connector = new SelectChannelConnector();
    server.addConnector(connector);
    flakyHandler = new FlakyHandler(4, true); // support ranges
    server.setHandler(flakyHandler);
    server.start();        
    
    File file = TestFileUtils.createTempFile("");
    file.delete();

    ArtifactDownload download = new ArtifactDownload(artifact, "", file, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    RemoteRepository repo = new RemoteRepository.Builder("test", "default", url()).build();
    RepositoryConnector connector = factory.newInstance(session, repo);
    try {
      connector.get(Arrays.asList(download), null);
    } finally {
      connector.close();
    }

    assertEquals("No exception resulted from downloading from bad server", download.getException().getClass(), ArtifactTransferException.class);
  }
 
  public void testRetryDownloadsWhereTheServerDropsTheConnectionAndDoesNotSupportsRanges() throws Exception {
    
    artifact = new DefaultArtifact("gid", "aid", "classifier", "extension", "version");

    server = new Server();
    connector = new SelectChannelConnector();
    server.addConnector(connector);
    FlakyServerHandlerWithNoRangeSupport flakyHandler = new FlakyServerHandlerWithNoRangeSupport(2); // do not support ranges
    server.setHandler(flakyHandler);
    server.start();
    
    File file = TestFileUtils.createTempFile("");
    file.delete();

    ArtifactDownload download = new ArtifactDownload(artifact, "", file, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    RemoteRepository repo = new RemoteRepository.Builder("test", "default", url()).build();
    RepositoryConnector connector = factory.newInstance(session, repo);
    try {
      connector.get(Arrays.asList(download), null);
    } finally {
      connector.close();
    }

    assertNull(String.valueOf(download.getException()), download.getException());
    assertTrue("Missing " + file.getAbsolutePath(), file.isFile());
    assertEquals("Bad size of " + file.getAbsolutePath(), flakyHandler.totalSize, file.length());
    assertContentPattern(file);
  }  
  
  @Override
  public void configure(Binder binder) {
    binder.bind(ILoggerFactory.class).to(SimpleLoggerFactory.class);
  }
}
