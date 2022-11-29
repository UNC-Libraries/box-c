package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.persist.api.PackagingType.METS_CDR;
import static edu.unc.lib.boxc.persist.api.PackagingType.SIMPLE_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
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

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/redis-server-context.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/ingest-it-servlet.xml")
})
public class IngestControllerIT extends AbstractAPIIT {
    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSITOR_EMAIL = "adminuser@example.com";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private File depositsDir;

    private PID destPid;

    @Autowired
    private CDRMETSDepositHandler metsHandler;
    @Autowired
    private SimpleObjectDepositHandler simpleHandler;

    @Autowired
    private DepositStatusFactory depositStatusFactory;

    @Before
    public void setup() throws Exception {
        destPid = makePid();

        tmpFolder.create();
        depositsDir = tmpFolder.newFolder("deposits");
        metsHandler.setDepositsDirectory(depositsDir);
        simpleHandler.setDepositsDirectory(depositsDir);

        AccessGroupSet testPrincipals = new AccessGroupSetImpl("admins");

        GroupsThreadStore.storeUsername(DEPOSITOR);
        GroupsThreadStore.storeGroups(testPrincipals);
        GroupsThreadStore.storeEmail(DEPOSITOR_EMAIL);
    }

    @After
    public void teardown() throws Exception {
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

        Map<String, Object> respMap = getMapFromResponse(result);
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

        Map<String, Object> respMap = getMapFromResponse(result);
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
        assertTrue("admins principal must be set in deposit", depositPrincipals.contains("admins"));
    }

    private void assertDepositFileStored(String depositId, String filename, String fileContent) throws Exception {
        File depositDir = new File(depositsDir, depositId);
        assertTrue("Deposit directory does not exist", depositDir.exists());
        File dataDir = new File(depositDir, "data");
        File originalFile = new File(dataDir, filename);
        assertEquals(fileContent, FileUtils.readFileToString(originalFile, "UTF-8"));
    }

    private InputStream getTestMETSInputStream() {
        return this.getClass().getResourceAsStream("/cdr_mets_package.xml");
    }
}
