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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import edu.unc.lib.dl.ui.exception.ClientAbortException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

public class FileIOUtil {
	
	public static void stream(OutputStream outStream, HttpMethodBase method) throws IOException{
		InputStream in = method.getResponseBodyAsStream();
		BufferedInputStream reader = null;
		try {
			reader = new BufferedInputStream(in);
			byte[] buffer = new byte[4096];
			int count = 0;
			int length = 0;
			while ((length = reader.read(buffer)) >= 0) {
				try {
					outStream.write(buffer, 0, length);
					if (count++ % 5 == 0){
						outStream.flush();
					}
				} catch (IOException e) {
					// Differentiate between socket being closed when writing vs reading
					throw new ClientAbortException(e);
				}
			}
			try {
				outStream.flush();
			} catch (IOException e) {
				throw new ClientAbortException(e);
			}
		} finally {
			if (reader != null)
				reader.close();
			if (in != null)
				in.close();
		}
	}
	
	public static String postImport(HttpServletRequest request, String url){
		Map<String, String[]> parameters = request.getParameterMap();
		HttpClientParams params = new HttpClientParams();
		params.setContentCharset("UTF-8");
		HttpClient client = new HttpClient();
		client.setParams(params);
		
		PostMethod post = new PostMethod(url);
		Iterator<Entry<String,String[]>> parameterIt = parameters.entrySet().iterator();
		while (parameterIt.hasNext()){
			Entry<String,String[]> parameter = parameterIt.next();
			for (String parameterValue: parameter.getValue()){
				post.addParameter(parameter.getKey(), parameterValue);
			}
		}
		
		try {
			client.executeMethod(post);
			return post.getResponseBodyAsString();
		} catch (Exception e) {
			throw new ResourceNotFoundException("Failed to retrieve POST import request for " + url, e);
		} finally {
			post.releaseConnection();
		}
	}
}
