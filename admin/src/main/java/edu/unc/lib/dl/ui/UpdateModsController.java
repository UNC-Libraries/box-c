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
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateModsController {
	private static final Log log = LogFactory.getLog(UpdateModsController.class);
	private String servicesUrl;
	private String username;
	private String password;
	private TripleStoreQueryService tripleStoreQueryService;

	@RequestMapping(value = "/admin/updatemods", method = RequestMethod.GET)
	public ModelAndView accessMods(HttpServletRequest request, @RequestParam String pid) {		
		// call mods editor
		String objectLabel = tripleStoreQueryService.lookupLabel(pid);
		ModelAndView mv = new ModelAndView("modseditor");
		mv.addObject("objectLabel", objectLabel);
	
		return mv;
	}
	
	@RequestMapping(value = "/admin/updatemods", method = RequestMethod.POST)
	public @ResponseBody
	String uploadMods(HttpServletRequest request, @RequestParam String pid) {
		String responseString;

		try {

			Abdera abdera = new Abdera();
			Entry entry = abdera.newEntry();
			Parser parser = abdera.getParser();
			Document<FOMExtensibleElement> doc = parser.parse(request.getInputStream());
			entry.addExtension(doc.getRoot());

			// entry.writeTo(System.out);

			String dataUrl = servicesUrl + "object/" + pid;

			HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
			client.getParams().setAuthenticationPreemptive(true);
			PutMethod method = new PutMethod(dataUrl);
			Header header = new Header("Content-Type", "application/atom+xml");
			method.setRequestHeader(header);
			StringWriter stringWriter = new StringWriter(2048);
			entry.writeTo(stringWriter);
			StringRequestEntity requestEntity = new StringRequestEntity(stringWriter.toString(), "application/atom+xml",
					"UTF-8");

			method.setRequestEntity(requestEntity);

			try {
				client.executeMethod(method);
				if (method.getStatusCode() == HttpStatus.SC_NO_CONTENT) { // success
					try {
						responseString = method.getResponseBodyAsString();
					} catch (IOException e) {
						log.info("Problem retrieving " + dataUrl + " for " + pid + ": " + e.getMessage());
					} finally {
						method.releaseConnection();
					}
				} else if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST) { // probably a validation problem
					try {
						responseString = method.getResponseBodyAsString();
						// log.warn(responseString);
						
						return responseString;
					} catch (IOException e) {
						log.info("Problem retrieving " + dataUrl + " for " + pid + ": " + e.getMessage());
					} finally {
						method.releaseConnection();
					}
				} else {
					// Retry server errors
					if (method.getStatusCode() == 500) {
						try {
							log.warn("Failed to upload MODS " + method.getURI().getURI());
						} catch (URIException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						throw new Exception("Failure to fedora content due to response of: "
								+ method.getStatusLine().toString() + "\nPath was: " + method.getURI().getURI());
					}
				}
			} catch (Exception e) {
				log.error("Error while attempting to stream Fedora content for " + pid, e);
			}

			return "";
		//	return new ModelAndView("admin", model);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	//	return new ModelAndView("admin", model);
	}

	public String getServicesUrl() {
		return servicesUrl;
	}

	public void setServicesUrl(String servicesUrl) {
		this.servicesUrl = servicesUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
