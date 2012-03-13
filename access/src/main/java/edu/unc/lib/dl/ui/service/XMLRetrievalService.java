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
package edu.unc.lib.dl.ui.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


public class XMLRetrievalService {
	public static Document getXMLDocument(String url) throws HttpException, IOException, JDOMException  {
		SAXBuilder builder = new SAXBuilder();
		
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);
		method.getParams().setParameter("http.socket.timeout", new Integer(2000));
		method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
	    		new DefaultHttpMethodRetryHandler(3, false));
		method.getParams().setParameter("http.useragent", "");
		
		InputStream responseStream = null;
		Document document = null;
		
		try {
			client.executeMethod(method);
			responseStream = method.getResponseBodyAsStream();
			document = builder.build(responseStream);
		} finally {
			if (responseStream != null)
				responseStream.close();
			method.releaseConnection();
		}
		
		return document;
	}
}
