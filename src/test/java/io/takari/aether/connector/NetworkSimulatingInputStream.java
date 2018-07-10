package io.takari.aether.connector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class NetworkSimulatingInputStream extends InputStream {
    private InputStream delegate;

    public NetworkSimulatingInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        try {
            Thread.sleep(new Random().nextInt(50));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.read(b);
    }

}
