package edu.unc.lib.boxc.deposit.impl.submit;

import static edu.unc.lib.boxc.persist.api.PackagingType.METS_CDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

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

    @Before
    public void init() throws Exception {
        initMocks(this);

        tmpFolder.create();
        depositsDir = tmpFolder.newFolder("deposits");

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

    @Test(expected = DepositException.class)
    public void testNoInputStream() throws Exception {
        DepositData deposit = new DepositData(null, FILENAME, MIMETYPE, METS_CDR,
                DEPOSIT_METHOD, depositingAgent);

        depositHandler.doDeposit(destPid, deposit);
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

    @Test(expected = DepositException.class)
    public void testInvalidMETS() throws Exception {
        InputStream fileStream = new ByteArrayInputStream("Not mets".getBytes());
        DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                METS_CDR, DEPOSIT_METHOD, depositingAgent);

        depositPid = depositHandler.doDeposit(destPid, deposit);
    }

    @Test(expected = DepositException.class)
    public void testDepositsDirNotAvailable() throws Exception {
        depositsDir.delete();

        InputStream fileStream = getMETSInputStream();

        DepositData deposit = new DepositData(fileStream, FILENAME, MIMETYPE,
                METS_CDR, DEPOSIT_METHOD, depositingAgent);

        depositHandler.doDeposit(destPid, deposit);
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
        assertTrue("admin principal must be set in deposit", depositPrincipals.contains("admin"));
        assertTrue("adminGroup principal must be set in deposit", depositPrincipals.contains("adminGroup"));
    }

    private InputStream getMETSInputStream() throws Exception {
        return this.getClass().getResourceAsStream("/cdr_mets_package.xml");
    }
}
