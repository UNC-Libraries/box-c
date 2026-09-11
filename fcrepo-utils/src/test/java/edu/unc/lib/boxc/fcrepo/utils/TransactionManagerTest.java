package edu.unc.lib.boxc.fcrepo.utils;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionManagerTest {
    private static final URI TX_URI = URI.create("http://localhost:48087/fcrepo/rest/fcr:tx/test-tx");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FcrepoClient client;
    @Mock
    private BinaryTransferService binaryTransferService;
    @Mock
    private FcrepoResponse response;

    private TransactionManager manager;

    @BeforeEach
    public void setUp() {
        manager = new TransactionManager();
        manager.setClient(client);
        manager.setBinaryTransferService(binaryTransferService);
    }

    @AfterEach
    public void tearDown() {
        FedoraTransaction.txUriThread.remove();
        FedoraTransaction.rootTxThread.remove();
    }

    @Test
    public void startTransactionReturnsTransactionUsingLocationHeader() throws Exception {
        when(client.post(any(URI.class)).perform()).thenReturn(response);
        when(response.getLocation()).thenReturn(TX_URI);

        FedoraTransaction transaction = manager.startTransaction();

        assertEquals(TX_URI, transaction.getTxUri());
        verify(client).post(any(URI.class));
        verify(response).close();
    }

    @Test
    public void startTransactionWrapsClientFailure() throws Exception {
        when(client.post(any(URI.class)).perform())
                .thenThrow(new FcrepoOperationFailedException(TX_URI, 500, "failure"));

        assertThrows(FedoraException.class, () -> manager.startTransaction());
    }

    @Test
    public void commitTransactionCommitsBinaryTransferOnNoContent() throws Exception {
        when(client.put(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        manager.commitTransaction(TX_URI);

        verify(binaryTransferService).commitTransaction(TX_URI);
        verify(binaryTransferService, never()).rollbackTransaction(TX_URI);
    }

    @Test
    public void commitTransactionRollsBackBinaryTransferOnFailure() throws Exception {
        when(client.put(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(response.getHeaderValues("Status")).thenReturn(Collections.singletonList("failure"));

        assertThrows(FedoraException.class, () -> manager.commitTransaction(TX_URI));

        verify(binaryTransferService).rollbackTransaction(TX_URI);
        verify(binaryTransferService, never()).commitTransaction(TX_URI);
    }

    @Test
    public void keepTransactionAliveSucceedsOnNoContent() throws Exception {
        when(client.post(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        manager.keepTransactionAlive(TX_URI);

        verify(client, times(2)).post(TX_URI);
    }

    @Test
    public void keepTransactionAliveThrowsOnUnexpectedStatus() throws Exception {
        when(client.post(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(response.getHeaderValues("Status")).thenReturn(Collections.singletonList("failure"));

        assertThrows(FedoraException.class, () -> manager.keepTransactionAlive(TX_URI));
    }

    @Test
    public void cancelTransactionAlwaysRollsBackBinaryTransfer() throws Exception {
        when(client.delete(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        manager.cancelTransaction(TX_URI);

        verify(binaryTransferService).rollbackTransaction(TX_URI);
    }

    @Test
    public void cancelTransactionRollsBackAndThrowsOnFailure() throws Exception {
        when(client.delete(TX_URI).perform()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(response.getHeaderValues("Status")).thenReturn(Collections.singletonList("failure"));

        assertThrows(FedoraException.class, () -> manager.cancelTransaction(TX_URI));

        verify(binaryTransferService).rollbackTransaction(TX_URI);
    }
}
