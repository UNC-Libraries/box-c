package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

public class AbstractSwordController extends AbstractSolrSearchController {
	private static final Logger log = LoggerFactory.getLogger(AbstractSwordController.class);

	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;

	public String updateDatastream(String pid, String datastream, HttpServletRequest request, HttpServletResponse response) {
		String responseString = null;
		String dataUrl = swordUrl + "object/" + pid;
		if (datastream != null)
			dataUrl += "/" + datastream;

		Abdera abdera = new Abdera();
		Entry entry = abdera.newEntry();
		Parser parser = abdera.getParser();
		Document<FOMExtensibleElement> doc;
		HttpClient client;
		PutMethod method;
		try {
			doc = parser.parse(request.getInputStream());
			entry.addExtension(doc.getRoot());

			client = HttpClientUtil.getAuthenticatedClient(dataUrl, swordUsername, swordPassword);
			client.getParams().setAuthenticationPreemptive(true);
			method = new PutMethod(dataUrl);
			// Pass the users groups along with the request
			method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());

			Header header = new Header("Content-Type", "application/atom+xml");
			method.setRequestHeader(header);
			StringWriter stringWriter = new StringWriter(2048);
			StringRequestEntity requestEntity;
			entry.writeTo(stringWriter);
			requestEntity = new StringRequestEntity(stringWriter.toString(), "application/atom+xml", "UTF-8");
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
			response.setStatus(method.getStatusCode());
			if (method.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				// success
				return "";
			} else if (method.getStatusCode() >= 400 && method.getStatusCode() <= 500) {
				if (method.getStatusCode() == 500)
					log.warn("Failed to upload " + datastream + " " + method.getURI().getURI());
				// probably a validation problem
				responseString = method.getResponseBodyAsString();
				return responseString;
			} else {
				response.setStatus(500);
				throw new Exception("Failure to update fedora content due to response of: " + method.getStatusLine().toString()
						+ "\nPath was: " + method.getURI().getURI());
			}
		} catch (Exception e) {
			log.error("Error while attempting to stream Fedora content for " + pid, e);
		} finally {
			if (method != null)
				method.releaseConnection();
		}
		return responseString;
	}
}
