package edu.unc.lib.boxc.operations.impl.mimetype;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.InvalidMimeTypeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml")
})
public class CorrectMimetypesServiceIT {
    private static final String USER_PRINC = "user";
    private static final String GRP_PRINC = "group";

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Mock
    private AccessControlService aclService;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @TempDir
    public Path tmpFolder;

    private AutoCloseable closeable;
    private CorrectMimetypesService service;
    private AgentPrincipals agent;
    private ContentRootObject contentRoot;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        TestHelper.setContentBase(baseAddress);

        service = new CorrectMimetypesService();
        service.setAclService(aclService);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setPremisLoggerFactory(premisLoggerFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setTransactionManager(txManager);

        agent = new AgentPrincipalsImpl(USER_PRINC, new AccessGroupSetImpl(GRP_PRINC));
        PID contentRootPid = RepositoryPaths.getContentRootPid();
        repoInitializer.initializeRepository();
        contentRoot = repoObjLoader.getContentRootObject(contentRootPid);
    }

    @AfterEach
    public void cleanup() throws Exception {
        closeable.close();
    }

    @Test
    public void testCorrectMimetypesUpdatesMultipleFiles() throws Exception {
        PID filePid1 = addFileObject("file1.tif", "image/png");
        PID filePid2 = addFileObject("file2.pdf", "image/jpeg");

        List<PID> updatedPids = service.correctMimetypes(
                csv(
                        filePid1.getId() + ",image/tiff",
                        filePid2.getId() + ",application/pdf"
                ),
                agent);

        assertEquals(List.of(filePid1, filePid2), updatedPids);

        assertOriginalFileMimetype(filePid1, "image/tiff");
        assertOriginalFileMimetype(filePid2, "application/pdf");

        verify(operationsMessageSender).sendUpdateDescriptionOperation(
                eq(agent.getUsername()),
                eq(Collections.singletonList(filePid1)));

        verify(operationsMessageSender).sendUpdateDescriptionOperation(
                eq(agent.getUsername()),
                eq(Collections.singletonList(filePid2)));
    }

    @Test
    public void testInvalidMimetype() throws Exception {
        PID filePid = addFileObject("file1.png", "image/png");

        var e = assertThrows(InvalidMimeTypeException.class, () -> {
            service.correctMimetypes(
                    csv(filePid.getId() + ",image"),
                    agent);
        });

        assertTrue(e.getMessage().contains("Invalid mimetype"));

        assertOriginalFileMimetype(filePid, "image/png");

        verifyNoInteractions(operationsMessageSender);
    }

    @Test
    public void testPermissionDenied() throws Exception {
        PID filePid = addFileObject("file1.tif", "image/png");

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(
                        eq("User does not have permissions to edit mimetypes"),
                        eq(filePid),
                        any(),
                        eq(Permission.editDescription));

        assertThrows(AccessRestrictionException.class, () -> {
            service.correctMimetypes(
                    csv(filePid.getId() + ",image/tiff"),
                    agent);
        });

        assertOriginalFileMimetype(filePid, "image/png");

        verifyNoInteractions(operationsMessageSender);
    }

    @Test
    public void nonFileObjectFailsTest() throws Exception {
        PID workPid = pidMinter.mintContentPid();;
        repoObjFactory.createWorkObject(workPid, null);

        var e = assertThrows(IllegalArgumentException.class, () -> {
            service.correctMimetypes(
                    csv(workPid.getId() + ",image/tiff"),
                    agent);
        });

        assertTrue(e.getMessage().contains("Cannot update mimetype for non-file object"));

        verifyNoInteractions(operationsMessageSender);
    }

    private InputStream csv(String... rows) {
        String body = String.join(",", CorrectMimetypesService.CSV_HEADERS) + "\n"
                + String.join("\n", rows);

        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    private PID addFileObject(String filename, String mimetype) throws Exception {
        PID workPid = pidMinter.mintContentPid();
        PID filePid = pidMinter.mintContentPid();

        Path sourceFile = tmpFolder.resolve(filename);
        Files.writeString(sourceFile, "test file content");

        WorkObject workObject = repoObjFactory.createWorkObject(workPid, null);

        workObject.addDataFile(
                filePid,
                sourceFile.toUri(),
                filename,
                mimetype,
                null,
                null,
                null);

        return repoObjLoader.getFileObject(filePid).getPid();
    }

    private void assertOriginalFileMimetype(PID filePid, String expectedMimetype) {
        FileObject fileObject = repoObjLoader.getFileObject(filePid);
        BinaryObject originalFile = fileObject.getOriginalFile();

        Model model = originalFile.getModel(true);
        Resource binaryResource = model.getResource(originalFile.getPid().getRepositoryPath());

        assertTrue(model.contains(binaryResource, CdrDeposit.mimetype, expectedMimetype),
                "Expected CdrDeposit.mimetype to be " + expectedMimetype);
    }
}
