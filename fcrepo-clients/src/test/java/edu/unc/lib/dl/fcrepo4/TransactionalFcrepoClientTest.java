package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.*;

import java.net.URI;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static edu.unc.lib.dl.test.TestHelpers.setField;

import edu.unc.lib.dl.fcrepo4.TransactionalFcrepoClient.TransactionalFcrepoClientBuilder;

public class TransactionalFcrepoClientTest extends AbstractFedoraTest {
	
	private TransactionalFcrepoClientBuilder builder = (TransactionalFcrepoClientBuilder) TransactionalFcrepoClient.client();
	private TransactionalFcrepoClient txClient;
	private FedoraTransaction tx;
	
	@Mock
	private HttpRequestBase request;
	@Mock
	private HttpClient httpClient;
	
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		URI uri = URI.create(FEDORA_BASE);
		tx = new FedoraTransaction(uri, repository);
		txClient = (TransactionalFcrepoClient) builder.build();
		setField(txClient, "httpclient", httpClient);
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
