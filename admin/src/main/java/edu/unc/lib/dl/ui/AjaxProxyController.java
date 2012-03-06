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
package edu.unc.lib.dl.ui;

import java.io.IOException;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Pedro Assuncao (assuncas@gmail.com)
 * @May 26, 2009
 * 
 */
@Controller
public class AjaxProxyController {
	private static final Log log = LogFactory.getLog(AjaxProxyController.class);
	String servicesUrl = null;

	public String getServicesUrl() {
		return servicesUrl;
	}

	public void setServicesUrl(String servicesUrl) {
		this.servicesUrl = servicesUrl;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = { "/services/rest", "/services/rest/*", "/services/rest/**/*" })
	public final void proxyAjaxCall(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = request.getRequestURI().replaceFirst(".*/services/rest", this.servicesUrl);
		if (request.getQueryString() != null)
			url = url + "?" + request.getQueryString();

		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		HttpClient client = new HttpClient();
		try {
			HttpMethod method = null;

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

			// Execute the method
			client.executeMethod(method);

			// Set the content type, as it comes from the server
			Header[] headers = method.getResponseHeaders();
			for (Header header : headers) {
				if ("Content-Type".equalsIgnoreCase(header.getName())) {
					response.setContentType(header.getValue());
				}
			}
			int b;
			while ((b = method.getResponseBodyAsStream().read()) != -1) {
				response.getOutputStream().write(b);
			}
			response.getOutputStream().flush();
		} catch (HttpException e) {
			writer.write(e.toString());
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			writer.write(e.toString());
			throw e;
		}
	}
}
