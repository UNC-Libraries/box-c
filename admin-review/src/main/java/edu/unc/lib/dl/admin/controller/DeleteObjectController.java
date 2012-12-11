package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

@Controller
public class DeleteObjectController {
	private static final Logger log = LoggerFactory.getLogger(DeleteObjectController.class);
	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;
	
	// TODO This controller should be replaced by a direct call to SWORD once group forwarding is fully up and running
	@RequestMapping(value = "delete/{prefix}/{id}", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> updateDescription(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {

		String responseString;
		String pid = idPrefix + ":" + id;
		String dataUrl = swordUrl + "object/" + pid;

		HttpClient client;
		DeleteMethod method;
		
		client = HttpClientUtil.getAuthenticatedClient(dataUrl, swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);
		method = new DeleteMethod(dataUrl);
		// Pass the users groups along with the request
		method.addRequestHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER, GroupsThreadStore.getGroupString());

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pid", pid);
		result.put("action", "delete");
		
		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				// success
				try {
					responseString = method.getResponseBodyAsString();
					result.put("timestamp", System.currentTimeMillis());
				} catch (IOException e) {
					log.info("Problem retrieving " + dataUrl + " for " + pid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
				// probably a validation problem
				try {
					responseString = method.getResponseBodyAsString();
					result.put("error", responseString);
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
		return result;
	}
}
