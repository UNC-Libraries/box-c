package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.unc.lib.dl.fcrepo4.TransactionalFcrepoClient.TransactionalFcrepoClientBuilder;

public class TransactionalFcrepoClientTest extends AbstractFedoraTest {
	
	private TransactionalFcrepoClientBuilder builder = (TransactionalFcrepoClientBuilder) TransactionalFcrepoClient.client();
	private TransactionalFcrepoClient txClient;
	private FedoraTransaction tx;
	
	@Mock
	private HttpRequestBase request;
	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private StatusLine statusLine;
	@Mock
	private CloseableHttpResponse httpResponse;
	
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		URI uri = URI.create(FEDORA_BASE);
		tx = new FedoraTransaction(uri, repository);
		txClient = (TransactionalFcrepoClient) builder.build();
		setField(txClient, "httpclient", httpClient);
		
		when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(httpResponse.getAllHeaders()).thenReturn(new Header[]{});
	}
	
	@Test
	public void executeRequestWithTxTest() throws Exception {
		URI txUri = null;
		try (FcrepoResponse response = txClient.executeRequest(tx.getTxUri(), request)) {
			txUri = response.getLocation();
		} catch (FcrepoOperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tx.close();
		
		assertNotNull(txUri);
		assertNotEquals(tx.getTxUri(), txUri);
	}

}
