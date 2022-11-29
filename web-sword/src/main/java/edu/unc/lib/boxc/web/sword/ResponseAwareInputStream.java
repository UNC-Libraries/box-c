package edu.unc.lib.boxc.web.sword;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * Input stream wrapper class for streams that originated from a HttpResponse which could not be closed at the time of
 * reading.
 * 
 * @author bbpennel
 * 
 */
public class ResponseAwareInputStream extends InputStream {
    private CloseableHttpResponse response;
    private InputStream originalStream;

    public ResponseAwareInputStream(CloseableHttpResponse resp) throws IOException {
        this.response = resp;
        this.originalStream = resp.getEntity().getContent();
    }

    @Override
    public int read() throws IOException {
        return originalStream.read();
    }

    public void close() throws IOException {
        if (response != null) {
            response.close();
        }
    }
}
