package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Controller
@RequestMapping(value = "{prefix}/{id}/describe")
public class MODSController {
	private static final Logger log = LoggerFactory.getLogger(MODSController.class);

	private String servicesUrl;
	private String username;
	private String password;
	private TripleStoreQueryService tripleStoreQueryService;

	@RequestMapping(method = RequestMethod.GET)
	public String editDescription(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {

		PID pid = new PID(idPrefix + ":" + id);
		String objectLabel = tripleStoreQueryService.lookupLabel(pid);
		model.addAttribute("objectLabel", objectLabel);
		return "edit/mods";
	}

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody
	String updateDescription(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {

		String responseString;
		String pid = idPrefix + ":" + id;
		String dataUrl = servicesUrl + "object/" + pid;
		
		Abdera abdera = new Abdera();
		Entry entry = abdera.newEntry();
		Parser parser = abdera.getParser();
		Document<FOMExtensibleElement> doc;
		HttpClient client;
		PutMethod method;
		try {
			doc = parser.parse(request.getInputStream());
			entry.addExtension(doc.getRoot());

			client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
			client.getParams().setAuthenticationPreemptive(true);
			method = new PutMethod(dataUrl);
			// Pass the users groups along with the request
			String groups = GroupsThreadStore.getGroups();
			method.addRequestHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER, groups);
			
			Header header = new Header("Content-Type", "application/atom+xml");
			method.setRequestHeader(header);
			StringWriter stringWriter = new StringWriter(2048);
			StringRequestEntity requestEntity;
			entry.writeTo(stringWriter);
			requestEntity = new StringRequestEntity(stringWriter.toString(), "application/atom+xml",
					"UTF-8");
			method.setRequestEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			log.error("Encoding not supported", e);
			return null;
		} catch (IOException e) {
			log.error("IOException while writing entry", e);
			return null;
		}

		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				// success
				try {
					responseString = method.getResponseBodyAsString();
				} catch (IOException e) {
					log.info("Problem retrieving " + dataUrl + " for " + pid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
				// probably a validation problem
				try {
					responseString = method.getResponseBodyAsString();

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
						log.error("Bad URI", e);
					}
				} else {
					throw new Exception("Failure to fedora content due to response of: " + method.getStatusLine().toString()
							+ "\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (Exception e) {
			log.error("Error while attempting to stream Fedora content for " + pid, e);
		}

		GroupsThreadStore.clearGroups();
		return "";
	}
}
