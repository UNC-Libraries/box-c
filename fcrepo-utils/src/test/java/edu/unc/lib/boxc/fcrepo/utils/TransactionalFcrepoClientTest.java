package edu.unc.lib.boxc.fcrepo.utils;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;

/**
 *
 * @author harring
 *
 */
public class TransactionalFcrepoClientTest {

    private static final String BASE_URI = "http://localhost:48085/rest/";
    private static final String TX_URI = "http://localhost:48085/rest/tx:99b58d30-06f5-477b-a44c-d614a9049d38";
    private static final String RESC_URI = "http://localhost:48085/rest/some/resource/id";
    private static final String REQUEST_URI =
            "http://localhost:48085/rest/tx:99b58d30-06f5-477b-a44c-d614a9049d38/some/resource/id";

    private TransactionalFcrepoClient txClient;
    private FedoraTransaction tx;
    private TransactionManager txManager;
    private AutoCloseable closeable;

    @Mock
    private HttpRequestBase request;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private StatusLine statusLine;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private Header header;
    @Mock
    private BinaryTransferService bts;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        URI uri = URI.create(TX_URI);
        FcrepoClientBuilder builder = TransactionalFcrepoClient.client(BASE_URI);
        txClient = (TransactionalFcrepoClient) builder.build();
        txManager= new TransactionManager();
        txManager.setClient(txClient);
        txManager.setBinaryTransferService(bts);
        tx = new FedoraTransaction(uri, txManager);

        setField(txClient, "httpclient", httpClient);

        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(header.getName()).thenReturn("Location");
        when(header.getValue())
            .thenReturn(REQUEST_URI);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header});
        when(request.getMethod()).thenReturn("GET");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void executeRequestWithTxTest() throws Exception {
        URI  rescUri = URI.create(RESC_URI);
        assertFalse(rescUri.toString().contains("tx:"));
        assertNotEquals(rescUri.toString(), REQUEST_URI);

        try (FcrepoResponse response = txClient.executeRequest(rescUri, request)) {
            rescUri = response.getLocation();
        } finally {
            tx.close();
        }

        assertTrue(rescUri.toString().contains("tx:"));
        assertEquals(rescUri.toString(), REQUEST_URI);
    }

}
