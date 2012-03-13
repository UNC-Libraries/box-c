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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

/**
 * Contains methods which are needed exclusively for the cdr jstl tag library
 * @author bbpennel
 *
 */
public class CDRTagLibUtil {
	public static String getDatastreamUrl(String pid, String datastream, FedoraUtil fedoraUtil){
		return fedoraUtil.getDatastreamUrl(pid, datastream);
	}
	
	public static void decrementLongMap(Map<String,Long> map, String key){
		if (!map.containsKey(key))
			return;
		map.put(key, map.get(key) - 1);
	}
	
	public static String postImport(HttpServletRequest request, String url){
		@SuppressWarnings("unchecked")
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
	
	public static String urlEncode(String value) throws UnsupportedEncodingException {
	    return URLEncoder.encode(value, "UTF-8");
	}

}
