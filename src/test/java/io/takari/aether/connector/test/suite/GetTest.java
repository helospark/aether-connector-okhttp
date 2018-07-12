/**
 * Copyright (c) 2012 to original author or authors All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.aether.connector.test.suite;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.TransferEvent;
import org.junit.Assert;
import org.junit.Test;

public class GetTest extends AetherTestCase {

  @Test
  public void testDownloadArtifact() throws Exception {

    addDelivery("gid/aid/version/aid-version-classifier.extension", "artifact");
    addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", sha1("artifact"));
    addDelivery("gid/aid/version/aid-version-classifier.extension.md5", md5("artifact"));

    File f = TestFileUtils.createTempFile("");
    Artifact a = artifact("bla");

    ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
    Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
    RepositoryConnector c = connector();
    c.get(downs, null);

    if (down.getException() != null) {
      down.getException().printStackTrace();
    }

    assertNull(String.valueOf(down.getException()), down.getException());
    assertContent("artifact", f);
  }

  @Test
  public void testDownloadArtifactChecksumFailure() throws Exception {

    addDelivery("gid/aid/version/aid-version-classifier.extension", "artifact");
    addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", "foo");
    addDelivery("gid/aid/version/aid-version-classifier.extension.md5", "bar");

    File f = TestFileUtils.createTempFile("");
    Artifact a = artifact("bla");

    ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
    Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
    connector().get(downs, null);

    assertNotNull(down.getException());
  }

  @Test
  public void testDownloadArtifactNoChecksumAvailable() throws Exception {
    addDelivery("gid/aid/version/aid-version-classifier.extension", "artifact");

    File f = TestFileUtils.createTempFile("");
    Artifact a = artifact("foo");

    ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
    Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
    connector().get(downs, null);

    assertContent("", f);
    assertNotNull(down.getException());
  }

  @Test
  public void testDownloadCorrupted() throws Exception {
    RecordingTransferListener transferListener = new RecordingTransferListener();

    addDelivery("gid/aid/version/aid-version-classifier.extension", "artifact");
    addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", "foo");
    addDelivery("gid/aid/version/aid-version-classifier.extension.md5", "bar");

    File f = TestFileUtils.createTempFile("");
    Artifact a = artifact("bla");

    ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_WARN);
    down.setListener(transferListener);
    Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
    connector().get(downs, null);

    TransferEvent corruptedEvent = null;
    for (TransferEvent e : transferListener.getEvents()) {
      if (TransferEvent.EventType.CORRUPTED.equals(e.getType())) {
        corruptedEvent = e;
        break;
      }
    }
    assertNotNull(corruptedEvent);
  }

  @Test
  public void testDownloadArtifactWithWait() throws Exception {
    addDelivery("gid/aid/version/aid-version-classifier.extension", "artifact");
    addDelivery("gid/aid/version/aid-version-classifier.extension.sha1", sha1("artifact"));
    addDelivery("gid/aid/version/aid-version-classifier.extension.md5", md5("artifact"));

    File f = TestFileUtils.createTempFile("");
    Artifact a = artifact("foo");

    ArtifactDownload down = new ArtifactDownload(a, null, f, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
    Collection<? extends ArtifactDownload> downs = Arrays.asList(down);
    connector().get(downs, null);

    assertNull(String.valueOf(down.getException()), down.getException());
    assertContent("foo", a.getFile());
    assertContent("artifact", f);
  }

  public static void assertContent(byte[] expected, File file) throws IOException {
    Assert.assertArrayEquals(expected, TestFileUtils.readBytes(file));
  }

  public static void assertContent(String expected, File file) throws IOException {
    byte[] content = TestFileUtils.readBytes(file);
    String msg = new String(content, "UTF-8");
    if (msg.length() > 10) {
      msg = msg.substring(0, 10) + "...";
    }
    Assert.assertArrayEquals("content was '" + msg + "'\n", expected.getBytes("UTF-8"), content);
  }

}
