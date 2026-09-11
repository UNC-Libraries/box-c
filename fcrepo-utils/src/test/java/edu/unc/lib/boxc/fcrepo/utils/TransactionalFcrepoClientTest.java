package edu.unc.lib.boxc.fcrepo.utils;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private static final String BASE_URI = "http://localhost:48087/fcrepo/rest/";
    private static final String TX_URI = "http://localhost:48087/fcrepo/rest/fcr:tx/99b58d30-06f5-477b-a44c-d614a9049d38";
    private static final String RESC_URI = "http://localhost:48087/fcrepo/rest/some/resource/id";
    private static final String REQUEST_URI = "http://localhost:48087/fcrepo/rest/fcr:tx";

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
        FcrepoClientBuilder builder = TransactionalFcrepoClient.client();
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
        when(header.getValue()).thenReturn(REQUEST_URI);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header});
        when(request.getMethod()).thenReturn("GET");
    }

    @AfterEach
    void closeService() throws Exception {
        FedoraTransaction.txUriThread.remove();
        closeable.close();
    }

    @Test
    public void executeRequestWithTxTest() throws Exception {
        URI  rescUri = URI.create(RESC_URI);
        assertFalse(rescUri.toString().contains("fcr:tx"));
        assertNotEquals(rescUri.toString(), REQUEST_URI);

        try (FcrepoResponse response = txClient.executeRequest(rescUri, request)) {
            rescUri = response.getLocation();
        } finally {
            tx.close();
        }

        assertTrue(rescUri.toString().contains("fcr:tx"));
        assertEquals(REQUEST_URI, rescUri.toString());
    }

    @Test
    public void executeRequestAddsAtomicIdForNonTransactionUri() throws Exception {
        FedoraTransaction.txUriThread.set(URI.create(TX_URI));

        txClient.executeRequest(URI.create(RESC_URI), request);

        verify(request).setHeader("Atomic-ID", TX_URI);
    }

    @Test
    public void executeRequestDoesNotAddAtomicIdForTransactionUri() throws Exception {
        txClient.executeRequest(URI.create(REQUEST_URI), request);

        verify(request, never()).setHeader(eq("Atomic-ID"), anyString());
    }

    @Test
    public void executeRequestDoesNotAddAtomicIdWithoutActiveTransaction() throws Exception {
        FedoraTransaction.txUriThread.remove();

        txClient.executeRequest(URI.create(RESC_URI), request);

        verify(request, never()).setHeader(eq("Atomic-ID"), anyString());
    }

    @Test
    public void builderBuildsTransactionalClientWithFluentConfiguration() {
        TransactionalFcrepoClient client = TransactionalFcrepoClient.client()
                .credentials("user", "password")
                .authScope("localhost")
                .throwExceptionOnFailure()
                .build();

        assertNotNull(client);
        assertInstanceOf(TransactionalFcrepoClient.class, client);
    }
}
