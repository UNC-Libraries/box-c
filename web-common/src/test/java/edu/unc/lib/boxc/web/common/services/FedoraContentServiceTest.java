package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
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
}
