package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FileVersionServiceTest {
    private AutoCloseable closeable;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse mockResponse;
    @Mock
    private GetBuilder getBuilder;
    @BeforeEach
    public void init() throws FcrepoOperationFailedException {
        closeable = openMocks(this);
        getBuilder = mock(GetBuilder.class, new SelfReturningAnswer());
        when(fcrepoClient.get(any(URI.class))).thenReturn(getBuilder);
        when(getBuilder.perform()).thenReturn(mockResponse);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
