package edu.unc.lib.dl.fcrepo4;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

public class FcrepoClientWrapper extends FcrepoClient {
	
	private static final String CREATE_TRANSACTION_SUFFIX = "/rest/fcr:tx";

	private String transactionId;

	protected FcrepoClientWrapper(String username, String password, String host, Boolean throwExceptionOnFailure) {
		super(username, password, host, throwExceptionOnFailure);
	}
	
	@Override
	public FcrepoResponse executeRequest(URI url, HttpRequestBase request)
            throws FcrepoOperationFailedException {
        
		return super.executeRequest(createTransactionEndpoint(url, request), request);
    }
	
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	private URI createTransactionEndpoint(URI url, HttpRequestBase request)
			throws FcrepoOperationFailedException {
		// appends suffix for creating transaction
		URI createTransUrl = url.resolve(CREATE_TRANSACTION_SUFFIX);
		// adds transaction creation uri to base uri
		URI fullRequestURI = request.getURI().resolve(createTransUrl);
		request.setURI(fullRequestURI);
		
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient client = builder.build();
		// attempts to create a transaction by making request to Fedora
		try (CloseableHttpResponse response = client.execute(request)) {
			// gets the full transaction uri from response header
			return URI.create(response.getFirstHeader("Location").toString());
		} catch (IOException e) {
			throw new FcrepoOperationFailedException(fullRequestURI, 400, "The request was invalid");
		}
					
	}
}
