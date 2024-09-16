package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class ImportThumbnailServiceTest {
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private AutoCloseable closeable;
    private ImportThumbnailService importThumbnailService;
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private CollectionObject collectionObject;
    @Mock
    private InputStream inputStream;
    @TempDir
    public Path tmpFolder;
    private Path tempStoragePath;


    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        tempStoragePath = tmpFolder.resolve("thumbnails");
        importThumbnailService = new ImportThumbnailService();
        importThumbnailService.setAclService(aclService);
        importThumbnailService.setMessageSender(messageSender);
        importThumbnailService.setTempStoragePath(tempStoragePath);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), any(), any(), eq(editDescription));

            importThumbnailService.run(inputStream, agent, COLLECTION_UUID, "image/jpeg");
        });
    }

    @Test
    public void successTest() throws IOException {
        var pid = PIDs.get(COLLECTION_UUID);
        importThumbnailService.run(inputStream, agent, COLLECTION_UUID, "image/jpeg");
        verify(messageSender).sendMakeThumbnailJP2Operation(agent.getUsername(), pid, tempStoragePath, "image/jpeg");
        assertTrue(Files.exists(tempStoragePath));
    }
}
