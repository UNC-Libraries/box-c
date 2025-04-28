package edu.unc.lib.boxc.deposit.impl.submit;

import static edu.unc.lib.boxc.persist.api.PackagingType.METS_CDR;
import static edu.unc.lib.boxc.persist.api.PackagingType.SIMPLE_OBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.PackagingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * @author bbpennel
 */
public class WorkFormDepositHandlerTest {
    private static final String FILENAME = "test.txt";
    private static final String MIMETYPE = "text/plain";
    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSITOR_EMAIL = "adminuser@example.com";
    private static final String FILE_CONTENT = "JSON Content";
    private static final String DEPOSIT_METHOD = "unitTest";

    @TempDir
    public Path tmpFolder;

    @Mock
    private PIDMinter pidMinter;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Captor
    private ArgumentCaptor<Map<String, String>> statusCaptor;

    private File depositsDir;

    private PID destPid;
    private PID depositPid;
    private AccessGroupSet testPrincipals;
    private AgentPrincipals depositingAgent;

    private WorkFormDepositorHandler depositHandler;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        Files.createDirectory(tmpFolder.resolve("deposits"));
        depositsDir = tmpFolder.resolve("deposits").toFile();

        destPid = PIDs.get(UUID.randomUUID().toString());
        depositPid = PIDs.get("deposit", UUID.randomUUID().toString());

        testPrincipals = new AccessGroupSetImpl("admin;adminGroup");
        depositingAgent = new AgentPrincipalsImpl(DEPOSITOR, testPrincipals);

        when(pidMinter.mintDepositRecordPid()).thenReturn(depositPid);

        depositHandler = new WorkFormDepositorHandler();
        depositHandler.setDepositsDirectory(depositsDir);
        depositHandler.setPidMinter(pidMinter);
        depositHandler.setDepositStatusFactory(depositStatusFactory);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testValidDeposit() throws Exception {
        InputStream fileStream = new ByteArrayInputStream(FILE_CONTENT.getBytes());
        DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                PackagingType.WORK_FORM_DEPOSIT, DEPOSIT_METHOD, depositingAgent);
        deposit.setDepositorEmail(DEPOSITOR_EMAIL);

        PID depositPid = depositHandler.doDeposit(destPid, deposit);

        verify(depositStatusFactory).save(eq(depositPid.getId()), statusCaptor.capture());
        Map<String, String> status = statusCaptor.getValue();

        verifyDepositFields(depositPid, status);
    }

    private void verifyDepositFields(PID depositPid, Map<String, String> status) {
        assertEquals(depositPid.getId(), status.get(RedisWorkerConstants.DepositField.uuid.name()));
        assertNotNull(status.get(RedisWorkerConstants.DepositField.submitTime.name()), "Deposit submission time must be set");
        assertEquals(FILENAME, status.get(RedisWorkerConstants.DepositField.fileName.name()));
        Path sourcePath = Paths.get(depositsDir.getAbsolutePath(), depositPid.getId(), "data", FILENAME);
        assertEquals(sourcePath.toUri().toString(), status.get(RedisWorkerConstants.DepositField.sourceUri.name()));
        assertEquals(MIMETYPE, status.get(RedisWorkerConstants.DepositField.fileMimetype.name()));
        assertEquals(PackagingType.WORK_FORM_DEPOSIT.getUri(), status.get(RedisWorkerConstants.DepositField.packagingType.name()));
        assertEquals(DEPOSIT_METHOD, status.get(RedisWorkerConstants.DepositField.depositMethod.name()));
        assertEquals(destPid.getId(), status.get(RedisWorkerConstants.DepositField.containerId.name()));
        assertEquals(RedisWorkerConstants.Priority.normal.name(), status.get(RedisWorkerConstants.DepositField.priority.name()));

        assertEquals(RedisWorkerConstants.DepositState.unregistered.name(), status.get(RedisWorkerConstants.DepositField.state.name()));
        assertEquals(RedisWorkerConstants.DepositAction.register.name(), status.get(RedisWorkerConstants.DepositField.actionRequest.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSetImpl(status.get(RedisWorkerConstants.DepositField.permissionGroups.name()));
        assertTrue(depositPrincipals.contains("admin"), "admin principal must be set in deposit");
        assertTrue(depositPrincipals.contains("adminGroup"), "adminGroup principal must be set in deposit");
    }
}
