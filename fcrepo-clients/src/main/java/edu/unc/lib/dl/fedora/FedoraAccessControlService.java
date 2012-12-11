package edu.unc.lib.dl.fedora;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * Retrieves the access control information for objects stored in Fedora.
 * 
 * @author bbpennel
 * 
 */
public class FedoraAccessControlService implements AccessControlService {

	public static final String ROLES_TO_GROUPS = "roles";
	private static final Logger log = LoggerFactory.getLogger(FedoraAccessControlService.class);

	private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;
	private HttpClient httpClient;
	private ObjectMapper mapper;
	private String aclEndpointUrl;
	private String username;
	private String password;

	public FedoraAccessControlService() {
		this.multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
		this.httpClient = new HttpClient(this.multiThreadedHttpConnectionManager);
		this.mapper = new ObjectMapper();
	}
	
	public void init() {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
      httpClient.getState().setCredentials(HttpClientUtil.getAuthenticationScope(aclEndpointUrl), creds);
	}

	public void destroy() {
		this.httpClient = null;
		this.mapper = null;
		if (this.multiThreadedHttpConnectionManager != null)
			this.multiThreadedHttpConnectionManager.shutdown();
	}

	/**
	 * @Inheritdoc
	 * 
	 *             Retrieves the access control from a Fedora JSON endpoint, represented by role to group relations.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ObjectAccessControlsBean getObjectAccessControls(PID pid) {
		GetMethod method = new GetMethod(this.aclEndpointUrl);
		method.setRequestHeader("Content-Type", "application/json");
		method.setQueryString(new NameValuePair[] { new NameValuePair("pid", pid.getPid()) });

		try {
			int statusCode = httpClient.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				Map<?, ?> result = (Map<?, ?>) mapper.readValue(method.getResponseBodyAsStream(), Object.class);
				Map<String, List<String>> roleMappings = (Map<String, List<String>>) result.get(ROLES_TO_GROUPS);

				return ObjectAccessControlsBean.createObjectAccessControlBean(pid, roleMappings);
			}
		} catch (HttpException e) {
			log.error("Failed to retrieve object access control for " + pid, e);
		} catch (IOException e) {
			log.error("Failed to retrieve object access control for " + pid, e);
		} finally {
			method.releaseConnection();
		}

		return null;
	}

	public void setAclEndpointUrl(String aclEndpointUrl) {
		this.aclEndpointUrl = aclEndpointUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
