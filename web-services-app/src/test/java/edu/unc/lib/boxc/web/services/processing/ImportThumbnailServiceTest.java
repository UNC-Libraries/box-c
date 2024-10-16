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
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequestSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private ThumbnailRequestSender messageSender;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private CollectionObject collectionObject;
    @Captor
    private ArgumentCaptor<ImportThumbnailRequest> requestCaptor;
    private InputStream inputStream;
    @TempDir
    public Path tmpFolder;
    private Path tempStoragePath;


    @BeforeEach
    public void init() throws FileNotFoundException {
        closeable = openMocks(this);
        tempStoragePath = tmpFolder.resolve("thumbnails");
        importThumbnailService = new ImportThumbnailService();
        importThumbnailService.setAclService(aclService);
        importThumbnailService.setMessageSender(messageSender);
        importThumbnailService.setSourceImagesDir(tempStoragePath.toString());
        importThumbnailService.init();
        inputStream = new FileInputStream("src/test/resources/__files/bunny.jpg");
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
    public void notAnImageTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            importThumbnailService.run(inputStream, agent, COLLECTION_UUID, "video/mp4");
        });
    }

    @Test
    public void successTest() throws IOException {
        var mimetype = "image/jpeg";
        importThumbnailService.run(inputStream, agent, COLLECTION_UUID, mimetype);
        verify(messageSender).sendToImportQueue(requestCaptor.capture());

        var request = requestCaptor.getValue();
        assertEquals(COLLECTION_UUID, request.getPidString());
        assertEquals(agent, request.getAgent());
        assertEquals(mimetype, request.getMimetype());
        assertTrue(Files.exists(tempStoragePath));
    }
}
