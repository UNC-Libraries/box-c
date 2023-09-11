package edu.unc.lib.boxc.deposit.impl.submit;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.api.submit.DepositHandler;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.UnsupportedPackagingTypeException;

/**
 *
 * @author bbpennel
 *
 */
public class DepositSubmissionServiceTest {
    private static final String FILENAME = "test.txt";
    private static final String MIMETYPE = "text/plain";
    private static final String DEPOSIT_METHOD = "unitTest";
    private static final Path FILEPATH = Paths.get(FILENAME);

    private AutoCloseable closeable;

    @Mock
    private AccessControlService aclService;

    @Mock
    private DepositHandler depositHandler1;
    @Mock
    private DepositHandler depositHandler2;
    @Mock
    private AgentPrincipals depositingAgent;
    @Mock
    private PID depositPid;
    @Mock
    private DepositStatusFactory depositStatusFactory;

    private Map<PackagingType, DepositHandler> packageHandlers;

    private DepositSubmissionService depositService;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        packageHandlers = new HashMap<>();
        packageHandlers.put(PackagingType.SIMPLE_OBJECT, depositHandler1);
        packageHandlers.put(PackagingType.BAGIT, depositHandler2);

        depositService = new DepositSubmissionService();
        depositService.setPackageHandlers(packageHandlers);
        depositService.setAclService(aclService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testSubmitDepositSimplePackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                PackagingType.SIMPLE_OBJECT,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);

        verify(depositHandler1).doDeposit(depositPid, deposit);
    }

    @Test
    public void testSubmitDepositBagitPackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                PackagingType.BAGIT,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);

        verify(depositHandler2).doDeposit(depositPid, deposit);
    }

    @Test
    public void testSubmitDepositUnregisteredPackage() throws Exception {
        Assertions.assertThrows(UnsupportedPackagingTypeException.class, () -> {
            DepositData deposit = new DepositData(FILEPATH.toUri(),
                    MIMETYPE,
                    PackagingType.ATOM,
                    DEPOSIT_METHOD,
                    depositingAgent);

            depositService.submitDeposit(depositPid, deposit);
        });
    }

    @Test
    public void testSubmitDepositNullPackage() throws Exception {
        Assertions.assertThrows(UnsupportedPackagingTypeException.class, () -> {
            DepositData deposit = new DepositData(FILEPATH.toUri(),
                    MIMETYPE,
                    null,
                    DEPOSIT_METHOD,
                    depositingAgent);

            depositService.submitDeposit(depositPid, deposit);
        });
    }
}
