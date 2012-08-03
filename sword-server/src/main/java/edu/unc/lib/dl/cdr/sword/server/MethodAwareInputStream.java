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

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.log4j.Logger;

/**
 * Input stream wrapper class for streams that originated from a HttpMethod which could not be closed at the time of
 * reading.
 * 
 * @author bbpennel
 * 
 */
public class MethodAwareInputStream extends InputStream {
	private static Logger log = Logger.getLogger(MethodAwareInputStream.class);
	
	private HttpMethodBase method;
	private InputStream originalStream;

	public MethodAwareInputStream(HttpMethodBase method) throws IOException {
		this.originalStream = method.getResponseBodyAsStream();
		this.method = method;
	}

	@Override
	public int read() throws IOException {
		return originalStream.read();
	}

	public void close() {
		if (method != null){
			method.releaseConnection();
		} else if (originalStream != null){
			try {
				originalStream.close();
			} catch (Exception e) {
				log.error("Failed to close original stream", e);
			}
		}
	}
}
