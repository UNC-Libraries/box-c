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
package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * @author Pedro Assuncao (assuncas@gmail.com)
 * @May 26, 2009
 * 
 */
@Controller
public class RESTProxyController {
	private static final Log log = LogFactory.getLog(RESTProxyController.class);
	@Autowired
	String servicesUrl = null;

	public String getServicesUrl() {
		return servicesUrl;
	}

	public void setServicesUrl(String servicesUrl) {
		this.servicesUrl = servicesUrl;
	}

	@RequestMapping(value = { "/services/rest", "/services/rest/*", "/services/rest/**/*" })
	public final void proxyAjaxCall(HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.debug("Prepending service url " + this.servicesUrl + " to " + request.getRequestURI());
		String url = request.getRequestURI().replaceFirst(".*/services/rest/?", this.servicesUrl);
		if (request.getQueryString() != null)
			url = url + "?" + request.getQueryString();

		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		HttpClient client = new HttpClient();
		HttpMethod method = null;
		try {
			log.debug("Proxying ajax request to services REST api via " + request.getMethod());
			// Split this according to the type of request
			if (request.getMethod().equals("GET")) {
				method = new GetMethod(url);
			} else if (request.getMethod().equals("POST")) {
				method = new PostMethod(url);
				// Set any eventual parameters that came with our original
				// request (POST params, for instance)
				Enumeration<String> paramNames = request.getParameterNames();
				while (paramNames.hasMoreElements()) {
					String paramName = paramNames.nextElement();
					((PostMethod) method).setParameter(paramName, request.getParameter(paramName));
				}
			} else {
				throw new NotImplementedException("This proxy only supports GET and POST methods.");
			}
			
			// Forward the user's groups along with the request
			method.addRequestHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER, GroupsThreadStore.getGroupString());
			method.addRequestHeader("On-Behalf-Of", GroupsThreadStore.getUsername());

			// Execute the method
			client.executeMethod(method);

			// Set the content type, as it comes from the server
			Header[] headers = method.getResponseHeaders();
			for (Header header : headers) {
				if ("Content-Type".equalsIgnoreCase(header.getName())) {
					response.setContentType(header.getValue());
				}
			}
			try (InputStream responseStream = method.getResponseBodyAsStream()) {
				int b;
				while ((b = responseStream.read()) != -1) {
					response.getOutputStream().write(b);
				}
			}
			response.getOutputStream().flush();
		} catch (HttpException e) {
			writer.write(e.toString());
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			writer.write(e.toString());
			throw e;
		} finally {
			if (method != null)
				method.releaseConnection();
		}
	}
}
