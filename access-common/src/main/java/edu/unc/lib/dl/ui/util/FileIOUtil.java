/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.client.methods.CloseableHttpResponse;

import edu.unc.lib.dl.ui.exception.ClientAbortException;

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
