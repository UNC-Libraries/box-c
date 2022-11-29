package edu.unc.lib.boxc.deposit.fcrepo4;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.ActivityMetricsClient;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.impl.events.PremisLoggerFactoryImpl;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 *
 * @author bbpennel
 *
 */
public class AbstractDepositJobTest {

    protected static final String FEDORA_BASE = "http://localhost:48085/rest/";
    protected static final String TX_URI = "http://localhost:48085/rest/tx:99b58d30-06f5-477b-a44c-d614a9049d38";

    @Mock
    protected RepositoryObjectDriver driver;
    @Mock
    protected TransactionManager txManager;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    protected File depositsDirectory;
    protected File depositDir;

    @Mock
    protected JobStatusFactory jobStatusFactory;
    @Mock
    protected DepositStatusFactory depositStatusFactory;
    @Mock
    protected PremisLoggerFactoryImpl premisLoggerFactory;
    @Mock
    protected PremisLogger premisLogger;
    @Mock
    protected PremisEventBuilder premisEventBuilder;
    @Mock
    protected ActivityMetricsClient metricsClient;
    @Mock
    protected Resource testResource;
    @Mock
    protected PIDMinter pidMinter;

    protected String jobUUID;

    protected String depositUUID;
    protected PID depositPid;
    protected String depositJobId;

    protected DepositModelManager depositModelManager;
    @Mock
    protected FedoraTransaction tx;

    private Set<String> completedIds;

    @Before
    public void initBase() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        when(premisLoggerFactory.createPremisLogger(any(PID.class), any(File.class)))
                .thenReturn(premisLogger);
        when(premisLoggerFactory.createPremisLogger(any(RepositoryObject.class), any(BinaryTransferSession.class)))
                .thenReturn(premisLogger);
        when(premisLogger.buildEvent(any(Resource.class))).thenReturn(premisEventBuilder);
        when(premisEventBuilder.addEventDetail(anyString(), Matchers.<Object>anyVararg())).thenReturn(premisEventBuilder);
        when(premisEventBuilder.addSoftwareAgent(any(PID.class))).thenReturn(premisEventBuilder);
        when(premisEventBuilder.create()).thenReturn(testResource);

        tmpFolder.create();
        depositsDirectory = tmpFolder.newFolder("deposits");

        jobUUID = UUID.randomUUID().toString();

        depositUUID = UUID.randomUUID().toString();
        depositDir = new File(depositsDirectory, depositUUID);
        depositDir.mkdir();
        depositPid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, depositUUID);

        depositModelManager = DepositModelManager.inMemoryManager();

        when(txManager.startTransaction()).thenReturn(tx);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new TransactionCancelledException("Tx cancelled",
                        invocation.getArgument(0));
            }
        }).when(tx).cancel(any(Exception.class));

        completedIds = new HashSet<>();

        when(depositStatusFactory.getState(anyString())).thenReturn(DepositState.running);

        doAnswer(invocation -> {
            String objId = invocation.getArgument(1);
            completedIds.add(objId);
            return null;
        }).when(jobStatusFactory).addObjectCompleted(anyString(), anyString());
        when(jobStatusFactory.objectIsCompleted(anyString(), anyString())).thenAnswer(invocation -> {
            String objId = invocation.getArgument(1);
            return completedIds.contains(objId);
        });
    }

    @After
    public void tearDown() {
        depositModelManager.close();
    }

    protected PID makePid() {
        return makePid(RepositoryPathConstants.CONTENT_BASE);
    }

    protected PID makePid(String qualifier) {
        String uuid = UUID.randomUUID().toString();
        return PIDs.get(qualifier + "/" + uuid);
    }
}
