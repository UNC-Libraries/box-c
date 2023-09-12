package edu.unc.lib.boxc.deposit.impl.submit;

import static edu.unc.lib.boxc.persist.api.PackagingType.METS_CDR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.Priority;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 *
 * @author bbpennel
 *
 */
public class CDRMETSDepositHandlerTest {

    private static final String FILENAME = "mets.xml";
    private static final String MIMETYPE = "application/xml";
    private static final String DEPOSITOR = "adminuser";
    private static final String DEPOSIT_METHOD = "unitTest";

    private AutoCloseable closeable;

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

    private CDRMETSDepositHandler depositHandler;

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

        depositHandler = new CDRMETSDepositHandler();
        depositHandler.setDepositsDirectory(depositsDir);
        depositHandler.setPidMinter(pidMinter);
        depositHandler.setDepositStatusFactory(depositStatusFactory);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoInputStream() throws Exception {
        Assertions.assertThrows(DepositException.class, () -> {
            DepositData deposit = new DepositData(null, FILENAME, MIMETYPE, METS_CDR,
                    DEPOSIT_METHOD, depositingAgent);

            depositHandler.doDeposit(destPid, deposit);
        });
    }

    @Test
    public void testValidDeposit() throws Exception {
        InputStream fileStream = getMETSInputStream();
        DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                METS_CDR, DEPOSIT_METHOD, depositingAgent);

        PID depositPid = depositHandler.doDeposit(destPid, deposit);

        verify(depositStatusFactory).save(eq(depositPid.getId()), statusCaptor.capture());
        Map<String, String> status = statusCaptor.getValue();

        verifyDepositFields(depositPid, status);
    }

    @Test
    public void testFilenameWithModifiers() throws Exception {
        String modifierFilename = ".././" + FILENAME;
        InputStream fileStream = getMETSInputStream();
        DepositData deposit = new DepositData(fileStream, modifierFilename, MIMETYPE,
                METS_CDR, DEPOSIT_METHOD, depositingAgent);

        PID depositPid = depositHandler.doDeposit(destPid, deposit);

        verify(depositStatusFactory).save(eq(depositPid.getId()), statusCaptor.capture());
        Map<String, String> status = statusCaptor.getValue();

        verifyDepositFields(depositPid, status);
    }

    @Test
    public void testInvalidMETS() throws Exception {
        Assertions.assertThrows(DepositException.class, () -> {
            InputStream fileStream = new ByteArrayInputStream("Not mets".getBytes());
            DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                    METS_CDR, DEPOSIT_METHOD, depositingAgent);

            depositPid = depositHandler.doDeposit(destPid, deposit);
        });
    }

    @Test
    public void testDepositsDirNotAvailable() throws Exception {
        Assertions.assertThrows(DepositException.class, () -> {
            depositsDir.delete();

            InputStream fileStream = getMETSInputStream();

            DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                    METS_CDR, DEPOSIT_METHOD, depositingAgent);

            depositHandler.doDeposit(destPid, deposit);
        });
    }

    private void verifyDepositFields(PID depositPid, Map<String, String> status) {
        assertEquals(depositPid.getId(), status.get(DepositField.uuid.name()));
        assertNotNull("Deposit submission time must be set", status.get(DepositField.submitTime.name()));
        assertEquals(FILENAME, status.get(DepositField.fileName.name()));
        Path sourcePath = Paths.get(depositsDir.getAbsolutePath(), depositPid.getId(), "data", FILENAME);
        assertEquals(sourcePath.toUri().toString(), status.get(DepositField.sourceUri.name()));
        assertEquals(MIMETYPE, status.get(DepositField.fileMimetype.name()));
        assertEquals(METS_CDR.getUri(), status.get(DepositField.packagingType.name()));
        assertEquals(DEPOSIT_METHOD, status.get(DepositField.depositMethod.name()));
        assertEquals(destPid.getId(), status.get(DepositField.containerId.name()));
        assertEquals(Priority.normal.name(), status.get(DepositField.priority.name()));

        assertEquals("http://cdr.unc.edu/METS/profiles/Simple", status.get(DepositField.packageProfile.name()));
        assertEquals("type", status.get(DepositField.metsType.name()));
        assertEquals("2016-12-16T19:11:50.145Z", status.get(DepositField.createTime.name()));
        assertEquals("CDR Test", status.get(DepositField.intSenderDescription.name()));

        assertEquals(DepositState.unregistered.name(), status.get(DepositField.state.name()));
        assertEquals(DepositAction.register.name(), status.get(DepositField.actionRequest.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSetImpl(status.get(DepositField.permissionGroups.name()));
        assertTrue(depositPrincipals.contains("admin"), "admin principal must be set in deposit");
        assertTrue(depositPrincipals.contains("adminGroup"), "adminGroup principal must be set in deposit");
    }

    private InputStream getMETSInputStream() throws Exception {
        return this.getClass().getResourceAsStream("/cdr_mets_package.xml");
    }
}
