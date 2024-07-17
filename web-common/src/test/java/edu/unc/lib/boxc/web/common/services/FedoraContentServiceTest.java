package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FedoraContentServiceTest {
    private AutoCloseable closeable;
    private FedoraContentService fedoraContentService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FileObject fileObject;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private GetBuilder builder;
    @Mock
    private FcrepoResponse fcrepoResponse;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        fedoraContentService = new FedoraContentService();
        fedoraContentService.setClient(fcrepoClient);
        fedoraContentService.setAccessControlService(accessControlService);
        fedoraContentService.setRepositoryObjectLoader(repositoryObjectLoader);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void streamDataWithBadDatastream() {
        var pid = makePid();

        Assertions.assertThrows(NotFoundException.class, () -> {
            fedoraContentService.streamData(pid, "completely bad", false, response, null);
        });
    }

    @Test
    public void streamDataWithExternalDatastream() {
        var pid = makePid();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            fedoraContentService.streamData(pid, "fulltext", false, response, null);
        });
    }

    @Test
    public void streamDataSuccess() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getOriginalFile()).thenReturn(binaryObject);
        when(binaryObject.getFilename()).thenReturn("Best Name");
        when(binaryObject.getPid()).thenReturn(pid);
        when(fcrepoClient.get(any())).thenReturn(builder);
        when(builder.perform()).thenReturn(fcrepoResponse);



        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, null);
    }
}
