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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.schema.FedoraDataResponse;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.UtilityMethods;

@Controller
public class ContentController {
	protected final Log logger = LogFactory.getLog(getClass());
	private UiWebService uiWebService;
	private String userName;
	private String password;

	@RequestMapping("/data/**/*")
	public void streamContent(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		
		FedoraDataResponse fedoraDataResponse = uiWebService
				.getFedoraDataUrlFromRequest(request, "test");

		// Send queries for non-existent pages to the collections page
		if (fedoraDataResponse == null) {
			return;
		}
		
		String dataUrl = fedoraDataResponse.getFedoraDataUrl();

		try {
			HttpClientParams params = new HttpClientParams();
			params.setContentCharset("UTF-8");
			HttpClient client = new HttpClient();
			client.setParams(params);

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					userName, password);
			client.getState().setCredentials(new AuthScope(null, 443), cred);
			client.getState().setCredentials(new AuthScope(null, 80), cred);

			if (logger.isDebugEnabled())
				logger.debug(fedoraDataResponse.getFedoraDataUrl());
			PostMethod httppost = new PostMethod(dataUrl);

			InputStream is = null;

			try {
				client.executeMethod(httppost);
				if (httppost.getStatusCode() == HttpStatus.SC_OK) {
					InputStream in = httppost.getResponseBodyAsStream();

					try {
						if (fedoraDataResponse.getMimeType() != null) {
							response.setContentType(fedoraDataResponse.getMimeType());
							
							logger.debug("Content type: "+fedoraDataResponse
									.getMimeType());
						}

						byte[] buffer = new byte[4096];
						BufferedInputStream reader = new BufferedInputStream(in);
						BufferedOutputStream writer = new BufferedOutputStream(
								response.getOutputStream());

						UtilityMethods.bufferedRead(reader, writer);
						reader.close();
					} catch (IOException e) {
						logger.info("Problem retrieving "+dataUrl+" perhaps the user cancelled the download?");
					} finally {
						if (in != null)
							in.close();
					}

				} else {
					logger.error("Unexpected failure: "
							+ httppost.getStatusLine().toString());
				}
			} finally {
				httppost.releaseConnection();
			}

		} catch (Exception e) {
			logger.error("problems", e);
		}
	}

	@RequestMapping("/data/")
	public void streamContent2(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		streamContent(request, response);
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
