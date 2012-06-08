package cdr.forms;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwordDepositHandler implements DepositHandler {
	
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(SwordDepositHandler.class);
	
	private String serviceUrl;
	private String username;
	private String password;
	public String getServiceUrl() {
		return serviceUrl;
	}
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
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
	
	/* (non-Javadoc)
	 * @see cdr.forms.DepositHandler#deposit(java.lang.String, java.lang.String, java.io.InputStream)
	 */
	@Override
	public DepositResult deposit(String containerPid, String modsXml, InputStream depositData) {
		Abdera abdera = new Abdera();
		Entry entry = abdera.newEntry();
		Parser parser = abdera.getParser();
		Document<FOMExtensibleElement> doc = parser.parse(new ByteArrayInputStream(modsXml.getBytes()));
		entry.addExtension(doc.getRoot());

		// entry.writeTo(System.out);

		String dataUrl = getServiceUrl() + "object/" + containerPid;

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
		DepositResult result = new DepositResult();
//		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
//		client.getParams().setAuthenticationPreemptive(true);
//		PutMethod method = new PutMethod(dataUrl);
//		Header header = new Header("Content-Type", "application/atom+xml");
//		method.setRequestHeader(header);
		return result;
	}
}
