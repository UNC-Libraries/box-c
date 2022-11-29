package edu.unc.lib.boxc.web.common.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.client.methods.CloseableHttpResponse;

import edu.unc.lib.boxc.web.common.exceptions.ClientAbortException;

/**
 * 
 * @author count0
 *
 */
public class FileIOUtil {
    private FileIOUtil() {
    }

    public static void stream(OutputStream outStream, CloseableHttpResponse resp)
            throws IOException {
               try (InputStream in = resp.getEntity().getContent();
                BufferedInputStream reader = new BufferedInputStream(in)) {
            byte[] buffer = new byte[4096];
            int count = 0;
            int length = 0;
            while ((length = reader.read(buffer)) >= 0) {
                try {
                    outStream.write(buffer, 0, length);
                    if (count++ % 5 == 0) {
                        outStream.flush();
                    }
                } catch (IOException e) {
                    // Differentiate between socket being closed when writing vs
                    // reading
                    throw new ClientAbortException(e);
                }
            }
            try {
                outStream.flush();
            } catch (IOException e) {
                throw new ClientAbortException(e);
            }
        }
    }
}
