package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.persist.api.PackagingType.METS_CDR;
import static edu.unc.lib.boxc.persist.api.PackagingType.SIMPLE_OBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.test.TestHelpers;
import edu.unc.lib.boxc.deposit.api.submit.DepositHandler;
import edu.unc.lib.boxc.deposit.impl.submit.DepositSubmissionService;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.web.common.auth.AccessLevel;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.DepositMethod;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.submit.CDRMETSDepositHandler;
import edu.unc.lib.boxc.deposit.impl.submit.SimpleObjectDepositHandler;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.PackagingType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import redis.clients.jedis.JedisPool;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("/spring-test/redis-server-context.xml")
public class IngestControllerIT {
    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSITOR_EMAIL = "adminuser@example.com";

    @TempDir
    public Path tmpFolder;
    private File depositsDir;
    private Path stagingPath;

    private PID destPid;

    private CDRMETSDepositHandler metsHandler;
    private SimpleObjectDepositHandler simpleHandler;
    private DepositSubmissionService depositSubmissionService;
    @Autowired
    private JedisPool jedisPool;
    private DepositStatusFactory depositStatusFactory;
    private RepositoryPIDMinter pidMinter;
    @Mock
    private AccessControlService aclService;
    @Mock
    private AccessLevel accessLevel;
    @InjectMocks
    private IngestController controller;
    private AutoCloseable closeable;
    private MockMvc mvc;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        pidMinter = new RepositoryPIDMinter();
        destPid = pidMinter.mintContentPid();

        depositsDir = tmpFolder.resolve("deposits").toFile();
        Files.createDirectory(tmpFolder.resolve("deposits"));
        stagingPath = tmpFolder.resolve("staging");
        Files.createDirectory(stagingPath);

        depositStatusFactory = new DepositStatusFactory();
        depositStatusFactory.setJedisPool(jedisPool);

        metsHandler = new CDRMETSDepositHandler();
        metsHandler.setPidMinter(pidMinter);
        metsHandler.setDepositStatusFactory(depositStatusFactory);
        metsHandler.setDepositsDirectory(depositsDir);
        simpleHandler = new SimpleObjectDepositHandler();
        simpleHandler.setDepositsDirectory(depositsDir);
        simpleHandler.setDepositStatusFactory(depositStatusFactory);
        simpleHandler.setPidMinter(pidMinter);

        depositSubmissionService = new DepositSubmissionService();
        depositSubmissionService.setAclService(aclService);
        Map<PackagingType, DepositHandler> handlerMap = Map.of(
                PackagingType.METS_CDR, metsHandler,
                PackagingType.SIMPLE_OBJECT, simpleHandler
        );
        depositSubmissionService.setPackageHandlers(handlerMap);

        AccessGroupSet testPrincipals = new AccessGroupSetImpl("admins");

        GroupsThreadStore.storeUsername(DEPOSITOR);
        GroupsThreadStore.storeGroups(testPrincipals);
        GroupsThreadStore.storeEmail(DEPOSITOR_EMAIL);

        TestHelpers.setField(controller, "uploadStagingPath", stagingPath);
        TestHelpers.setField(controller, "depositService", depositSubmissionService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    public void teardownLocal() throws Exception {
        jedisPool.getResource().flushAll();
        closeable.close();
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testInsufficientPermissions() throws Exception {
        MockMultipartFile depositFile = new MockMultipartFile("file", "test.txt", "text/plain", "some text".getBytes());

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destPid), any(AccessGroupSetImpl.class), eq(Permission.ingest));

        mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .file(depositFile)
                .param("type", PackagingType.SIMPLE_OBJECT.getUri()))
                .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void testSimplePackage() throws Exception {
        String filename = "test.txt";
        String mimetype = "text/plain";
        String slug = "test_slug";
        String fileContent = "some text";

        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent.getBytes());

        MvcResult result = mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .file(depositFile)
                .param("name", slug)
                .param("type", PackagingType.SIMPLE_OBJECT.getUri()))
                .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(destPid.getId(), respMap.get("destination"));
        assertEquals("ingest", respMap.get("action"));
        String depositId = (String) respMap.get("depositId");
        assertNotNull(depositId);

        Map<String, String> status = depositStatusFactory.get(depositId);
        assertEquals(destPid.getId(), status.get(DepositField.containerId.name()));
        assertEquals(DepositMethod.CDRAPI1.getLabel(), status.get(DepositField.depositMethod.name()));

        assertEquals(filename, status.get(DepositField.fileName.name()));
        assertEquals(mimetype, status.get(DepositField.fileMimetype.name()));
        assertEquals(SIMPLE_OBJECT.getUri(), status.get(DepositField.packagingType.name()));

        assertEquals(slug, status.get(DepositField.depositSlug.name()));

        assertDepositorDetailsStored(status);

        assertDepositFileStored(depositId, filename, fileContent);
    }

    @Test
    public void testMETSPackage() throws Exception {
        String filename = "mets.xml";
        String mimetype = "application/xml";
        InputStream fileContent = getTestMETSInputStream();

        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent);

        MvcResult result = mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .file(depositFile)
                .param("type", PackagingType.METS_CDR.getUri()))
                .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(destPid.getId(), respMap.get("destination"));
        assertEquals("ingest", respMap.get("action"));
        String depositId = (String) respMap.get("depositId");
        assertNotNull(depositId);

        Map<String, String> status = depositStatusFactory.get(depositId);
        assertEquals(filename, status.get(DepositField.fileName.name()));
        assertEquals(mimetype, status.get(DepositField.fileMimetype.name()));
        assertEquals(METS_CDR.getUri(), status.get(DepositField.packagingType.name()));

        assertDepositorDetailsStored(status);

        String metsContent = IOUtils.toString(getTestMETSInputStream(), "UTF-8");
        assertDepositFileStored(depositId, filename, metsContent);
    }

    @Test
    public void testUnsupportedPackage() throws Exception {
        String filename = "test.txt";
        String mimetype = "text/plain";
        String fileContent = "some text";

        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent.getBytes());

        mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .file(depositFile)
                .param("type", PackagingType.SIMPLE_ZIP.getUri()))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testSimpleNoFile() throws Exception {
        mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .param("type", PackagingType.SIMPLE_OBJECT.getUri()))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testSubmissionFailure() throws Exception {
        String filename = "mets.xml";
        String mimetype = "application/xml";
        String fileContent = "Not really mets";

        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent.getBytes());

        mvc.perform(multipart("/edit/ingest/" + destPid.getId())
                .file(depositFile)
                .param("type", PackagingType.METS_CDR.getUri()))
                .andExpect(status().is5xxServerError())
            .andReturn();
    }

    private void assertDepositorDetailsStored(Map<String, String> status) {
        assertEquals(DEPOSITOR, status.get(DepositField.depositorName.name()));
        assertEquals(DEPOSITOR_EMAIL, status.get(DepositField.depositorEmail.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSetImpl(status.get(DepositField.permissionGroups.name()));
        assertTrue(depositPrincipals.contains("admins"), "admins principal must be set in deposit");
    }

    private void assertDepositFileStored(String depositId, String filename, String fileContent) throws Exception {
        File depositDir = new File(depositsDir, depositId);
        assertTrue(depositDir.exists(), "Deposit directory does not exist");
        File dataDir = new File(depositDir, "data");
        File originalFile = new File(dataDir, filename);
        assertEquals(fileContent, FileUtils.readFileToString(originalFile, "UTF-8"));
    }

    private InputStream getTestMETSInputStream() {
        return this.getClass().getResourceAsStream("/cdr_mets_package.xml");
    }

    @Test
    public void testStageAndRemoveFile() throws Exception {
        String filename = "test.txt";
        String mimetype = "text/plain";
        String fileContent = "some text";

        when(accessLevel.isViewAdmin()).thenReturn(true);
        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent.getBytes());

        MvcResult result = mvc.perform(multipart("/edit/ingest/stageFile")
                        .file(depositFile)
                        .param("formKey", "")
                        .param("path", "")
                        .sessionAttr("accessLevel", accessLevel))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        var stagedTmp = (String) respMap.get("tmp");
        var stagedPath = stagingPath.resolve(stagedTmp);
        assertTrue(stagedPath.startsWith(stagingPath));
        assertTrue(Files.exists(stagedPath), "Staged file does not exist");
        assertEquals(filename, respMap.get("originalName"));

        try (Stream<Path> pathStream = Files.list(stagingPath)) {
            assertEquals(1, pathStream.count(), "Staging directory should only have one file");
        }

        String removeBody = "{\"file\": \"" + stagedTmp + "\", \"formKey\": \"testform\", \"path\": \"\"}";
        mvc.perform(post("/edit/ingest/removeStagedFile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removeBody)
                        .sessionAttr("accessLevel", accessLevel))
                .andExpect(status().isOk())
                .andReturn();

        try (Stream<Path> pathStream = Files.list(stagingPath)) {
            assertEquals(0, pathStream.count(), "Staging directory must now be empty");
        }
    }

    @Test
    public void testStageInsufficientPermissions() throws Exception {
        String filename = "test.txt";
        String mimetype = "text/plain";
        String fileContent = "some text";

        MockMultipartFile depositFile = new MockMultipartFile("file", filename, mimetype, fileContent.getBytes());

        when(accessLevel.isViewAdmin()).thenReturn(false);

        mvc.perform(multipart("/edit/ingest/stageFile")
                        .file(depositFile)
                        .param("formKey", "")
                        .param("path", "")
                        .sessionAttr("accessLevel", accessLevel))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testRemoveStagedInsufficientPermissions() throws Exception {
        when(accessLevel.isViewAdmin()).thenReturn(false);

        String removeBody = "{\"file\": \"test_file\", \"formKey\": \"testform\", \"path\": \"\"}";
        mvc.perform(post("/edit/ingest/removeStagedFile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removeBody)
                        .sessionAttr("accessLevel", accessLevel))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testRemoveStagedInvalidPath() throws Exception {
        when(accessLevel.isViewAdmin()).thenReturn(true);

        String removeBody = "{\"file\": \"../test_file\", \"formKey\": \"testform\", \"path\": \"\"}";
        mvc.perform(post("/edit/ingest/removeStagedFile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removeBody)
                        .sessionAttr("accessLevel", accessLevel))
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}
