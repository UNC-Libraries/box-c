package cdr.forms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.util.EntityProviderRequestEntity;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

public class SwordDepositRequest {
	
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(SwordDepositRequest.class);
	
	private String servicesUrl;
	private String username;
	private String password;
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
	
	public boolean execute(String containerPid, String modsXml, InputStream depositData) {
		Abdera abdera = new Abdera();
		Entry entry = abdera.newEntry();
		Parser parser = abdera.getParser();
		Document<FOMExtensibleElement> doc = parser.parse(new ByteArrayInputStream(modsXml.getBytes()));
		entry.addExtension(doc.getRoot());

		// entry.writeTo(System.out);

		String dataUrl = servicesUrl + "object/" + containerPid;

		AbderaClient client = new AbderaClient(abdera);
		Credentials creds = new UsernamePasswordCredentials(this.getUsername(), this.getPassword());
		try {
			client.addCredentials(null, null, null, creds);
		} catch (URISyntaxException e) {
			throw new Error("bad URI for SWORD credentials", e);
		}
		ClientResponse response = client.post(dataUrl, entry, depositData);
		LOG.debug(String.valueOf(response.getStatus()));
		LOG.debug(response.getStatusText());
		LOG.debug(response.getEntityTag().toString());
		LOG.debug(response.getLastModified().toString());
		LOG.debug(response.getContentType().toString());
//		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
//		client.getParams().setAuthenticationPreemptive(true);
//		PutMethod method = new PutMethod(dataUrl);
//		Header header = new Header("Content-Type", "application/atom+xml");
//		method.setRequestHeader(header);

	}
}
