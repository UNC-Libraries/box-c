package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * Controller for handling forms which submit ingests to SWORD
 * 
 * @author bbpennel
 * 
 */
@Controller
public class IngestController {
	private static final Logger log = LoggerFactory.getLogger(IngestController.class);

	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;
	private static QName SWORD_VERBOSE_DESCRIPTION = new QName("http://purl.org/net/sword/terms/", "verboseDescription");

	@RequestMapping(value = "ingest/{pid}", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, ? extends Object> ingestPackageController(@PathVariable("pid") String pid,
			@RequestParam("type") String type, @RequestParam(value = "name", required = false) String name,
			@RequestParam("file") MultipartFile ingestFile, HttpServletResponse response) {
		String destinationUrl = swordUrl + "collection/" + pid;
		HttpClient client = HttpClientUtil.getAuthenticatedClient(destinationUrl, swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);
		PostMethod method = new PostMethod(destinationUrl);

		// Set SWORD related headers for performing ingest
		method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());
		method.addRequestHeader("Packaging", type);
		method.addRequestHeader("On-Behalf-Of", GroupsThreadStore.getUsername());
		method.addRequestHeader("Content-Type", ingestFile.getContentType());
		method.addRequestHeader("Content-Length", Long.toString(ingestFile.getSize()));
		method.addRequestHeader("Content-Disposition", "attachment; filename=" + ingestFile.getOriginalFilename());
		if (name != null && name.trim().length() > 0)
			method.addRequestHeader("Slug", name);

		// Setup the json response
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("action", "ingest");
		result.put("destination", pid);
		try {
			method.setRequestEntity(new InputStreamRequestEntity(ingestFile.getInputStream(), ingestFile.getSize()));
			client.executeMethod(method);
			response.setStatus(method.getStatusCode());

			// Object successfully "create", or at least queued
			if (method.getStatusCode() == 201) {
				Header location = method.getResponseHeader("Location");
				String newPid = location.getValue();
				newPid = newPid.substring(newPid.lastIndexOf('/'));
				result.put("pid", newPid);
			} else if (method.getStatusCode() == 401) {
				// Unauthorized
				result.put("error", "Not authorized to ingest to container " + pid);
			} else if (method.getStatusCode() >= 500) {
				// Server error, report it to the client
				result.put("error", "A server error occurred while attempting to ingest \"" + ingestFile.getName()
						+ "\" to " + pid);

				// Inspect the SWORD response, extracting the stacktrace
				InputStream entryPart = method.getResponseBodyAsStream();
				Abdera abdera = new Abdera();
				Parser parser = abdera.getParser();
				Document<Entry> entryDoc = parser.parse(entryPart);
				Object rootEntry = entryDoc.getRoot();
				String stackTrace;
				if (rootEntry instanceof FOMExtensibleElement) {
					stackTrace = ((org.apache.abdera.parser.stax.FOMExtensibleElement) entryDoc.getRoot()).getExtension(
							SWORD_VERBOSE_DESCRIPTION).getText();
					result.put("errorStack", stackTrace);
				} else {
					stackTrace = ((Entry) rootEntry).getExtension(SWORD_VERBOSE_DESCRIPTION).getText();
					result.put("errorStack", stackTrace);
				}
				log.warn(
						"Failed to upload ingest package file " + ingestFile.getName() + " from user "
								+ GroupsThreadStore.getUsername(), stackTrace);
			}
			return result;
		} catch (Exception e) {
			log.warn("Encountered an unexpected error while ingesting package " + ingestFile.getName() + " from user "
					+ GroupsThreadStore.getUsername(), e);
			result.put("error", "A server error occurred while attempting to ingest \"" + ingestFile.getName() + "\" to "
					+ pid);
			return result;
		} finally {
			method.releaseConnection();
			try {
				ingestFile.getInputStream().close();
			} catch (IOException e) {
				log.warn("Failed to close ingest package file", e);
			}
		}
	}
}
