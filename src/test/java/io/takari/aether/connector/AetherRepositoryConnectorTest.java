package io.takari.aether.connector;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.takari.aether.client.AetherClient;
import io.takari.aether.client.Response;
import io.takari.aether.connector.test.suite.AetherConnectorTest;

public class AetherRepositoryConnectorTest {
    private static final int NUMBER_OF_THREADS = 4;
    private static final File rootDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "aether_test");
    
    private FileProcessor fileProcessor;
    private RemoteRepository repository;
    @Mock
    private RepositorySystemSession session;
    private ArtifactDownload artifactDownload;
    @Mock
    private AetherClient aetherClient;
    @Mock
    private RepositoryLayout repositoryLayout;
    
    private AetherRepositoryConnector aetherRepositoryConnector;

    @Before
    public void setUp() {
        for (File file : rootDir.listFiles()) {
            file.delete();
        }
    }
    
    @Test
    public void testConcurrency() throws NoRepositoryConnectorException, IOException {
        initMocks(this);
        
        fileProcessor = new DefaultFileProcessor();

        repository = new RemoteRepository.Builder("id", "type", "http://someArtifactory.com").setContentType("default").build();

        artifactDownload = new ArtifactDownload();
        artifactDownload.setArtifact(new DefaultArtifact("com", "example", "", "1.0"));
        artifactDownload.setExistenceCheck(false);
        artifactDownload.setChecksumPolicy("warn");
        artifactDownload.setFile(new File(rootDir, "somefile.jar"));

        aetherRepositoryConnector = new AetherRepositoryConnector(repository, session, fileProcessor);
        aetherRepositoryConnector.setAetherClient(aetherClient);

        given(aetherClient.get(anyString())).willAnswer(new FileAnswer());
        given(aetherClient.get(anyString(), any(Map.class))).willAnswer(new FileAnswer());

       
        doConcurrentCalls();
    }

    private void doConcurrentCalls() {
        
        // Create threads
        
        List<Thread> threads = IntStream.range(0, NUMBER_OF_THREADS)
                .boxed()
                .map(i -> new Thread(() ->  download()))
                .collect(Collectors.toList());

        // Start threads
        threads.stream()
                .forEach(thread -> thread.start());

        // Wait for them to finish
        threads.stream()
                .forEach(thread -> {
                    try {
                        thread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
       
    }

    private void download() {
        try {
            Thread.sleep(new Random().nextInt(500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        aetherRepositoryConnector.get(Arrays.asList(artifactDownload), new ArrayList<MetadataDownload>());
    }

    private static InputStream inputStreamFromClasspathFile(String string) throws URISyntaxException, IOException {
        Path uri = Paths.get(AetherConnectorTest.class.getResource("/" + string).toURI());
        return new NetworkSimulatingInputStream(new ByteArrayInputStream(Files.readAllBytes(uri)));
    }

    static class FileAnswer implements Answer<Response> {

        @Override
        public Response answer(InvocationOnMock invocation) throws Throwable {
            String fileToDownload = "test.jar";
            if (String.valueOf(invocation.getArguments()[0]).endsWith("sha1")) {
                fileToDownload = "test.jar.sha1";
            }
            InputStream inputStream = inputStreamFromClasspathFile(fileToDownload);
            if (invocation.getArguments().length > 1) {
                Map<String, String> map = (Map<String, String>) invocation.getArguments()[1];
                String range = map.get("Range");

                Pattern pattern = Pattern.compile("bytes=(\\d+)-");
                Matcher matcher = pattern.matcher(range);
                matcher.find();
                Integer start = Integer.valueOf(matcher.group(1));
                System.out.println("Start: " + start);
                inputStream.skip(start);
            }
            Response mockResponse = mock(Response.class);
            given(mockResponse.getStatusCode()).willReturn(200);
            given(mockResponse.getInputStream()).willReturn(inputStream);
            return mockResponse;
        }

    }
}
