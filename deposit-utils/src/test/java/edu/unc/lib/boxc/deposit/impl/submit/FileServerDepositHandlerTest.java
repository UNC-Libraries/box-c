package edu.unc.lib.boxc.deposit.impl.submit;

import static edu.unc.lib.boxc.persist.api.PackagingType.BAGIT;
import static edu.unc.lib.boxc.persist.api.PackagingType.DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.Priority;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.PackagingType;

public class FileServerDepositHandlerTest {

    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSIT_METHOD = "unitTest";
    private static final String DEPOSITOR_EMAIL = "adminuser@example.com";

    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;
    private Path sourcePath;

    @Mock
    private PIDMinter pidMinter;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Captor
    private ArgumentCaptor<Map<String, String>> statusCaptor;
    @Mock
    private DepositOperationMessageService operationMessageService;

    private PID destPid;
    private PID depositPid;

    private AgentPrincipals depositingAgent;
    private AccessGroupSet testPrincipals;

    private FileServerDepositHandler depositHandler;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        sourcePath = tmpFolder.resolve("source");
        Files.createDirectory(sourcePath);

        depositPid = PIDs.get("deposit", UUID.randomUUID().toString());
        destPid = PIDs.get(UUID.randomUUID().toString());

        testPrincipals = new AccessGroupSetImpl("admin;adminGroup");
        depositingAgent = new AgentPrincipalsImpl(DEPOSITOR, testPrincipals);

        when(pidMinter.mintDepositRecordPid()).thenReturn(depositPid);

        depositHandler = new FileServerDepositHandler();
        depositHandler.setPidMinter(pidMinter);
        depositHandler.setDepositStatusFactory(depositStatusFactory);
        depositHandler.setDepositOperationMessageService(operationMessageService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testDepositBagPackage() throws Exception {
        DepositData deposit = new DepositData(sourcePath.toUri(), null,
                BAGIT, DEPOSIT_METHOD, depositingAgent);
        deposit.setDepositorEmail(DEPOSITOR_EMAIL);

        depositHandler.doDeposit(destPid, deposit);

        verify(depositStatusFactory).save(eq(depositPid.getId()), statusCaptor.capture());
        Map<String, String> status = statusCaptor.getValue();

        verifyDepositFields(depositPid, BAGIT, status);
        verify(operationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    public void testDepositDirectory() throws Exception {
        DepositData deposit = new DepositData(sourcePath.toUri(), null,
                DIRECTORY, DEPOSIT_METHOD, depositingAgent);
        deposit.setDepositorEmail(DEPOSITOR_EMAIL);

        depositHandler.doDeposit(destPid, deposit);

        verify(depositStatusFactory).save(eq(depositPid.getId()), statusCaptor.capture());
        Map<String, String> status = statusCaptor.getValue();

        verifyDepositFields(depositPid, DIRECTORY, status);
        verify(operationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    private void verifyDepositFields(PID depositPid, PackagingType packageType,
            Map<String, String> status) {
        assertEquals(depositPid.getId(), status.get(DepositField.uuid.name()));
        assertNotNull(status.get(DepositField.submitTime.name()), "Deposit submission time must be set");
        assertEquals(packageType.getUri(), status.get(DepositField.packagingType.name()));
        assertEquals(DEPOSIT_METHOD, status.get(DepositField.depositMethod.name()));
        assertEquals(DEPOSITOR, status.get(DepositField.depositorName.name()));
        assertEquals(DEPOSITOR_EMAIL, status.get(DepositField.depositorEmail.name()));
        assertEquals(destPid.getId(), status.get(DepositField.containerId.name()));
        assertEquals(Priority.normal.name(), status.get(DepositField.priority.name()));

        assertEquals(DepositState.unregistered.name(), status.get(DepositField.state.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSetImpl(status.get(DepositField.permissionGroups.name()));
        assertTrue(depositPrincipals.contains("admin"), "admin principal must be set in deposit");
        assertTrue(depositPrincipals.contains("adminGroup"), "adminGroup principal must be set in deposit");
    }
}
