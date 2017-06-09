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
package edu.unc.lib.dl.cdr.sword.server;

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
