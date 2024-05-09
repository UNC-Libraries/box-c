package edu.unc.lib.boxc.deposit.fcrepo4;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.ActivityMetricsClient;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public abstract class AbstractFedoraDepositJobIT {

    @Autowired
    protected String serverAddress;
    @Autowired
    protected String baseAddress;
    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected DepositModelManager depositModelManager;
    @Autowired
    protected JobStatusFactory jobStatusFactory;
    @Autowired
    protected DepositStatusFactory depositStatusFactory;
    @Autowired
    protected PremisLoggerFactory premisLoggerFactory;
    @Autowired
    protected FcrepoClient client;
    @Autowired
    protected ActivityMetricsClient metricsClient;
    @Autowired
    protected BinaryTransferService binaryTransferService;
    @Autowired
    protected StorageLocationManager storageLocationManager;
    @Autowired
    protected StorageLocationTestHelper storageLocationTestHelper;
    @Autowired
    protected FcrepoClient fcrepoClient;
    @TempDir
    public Path tmpFolder;
    @Autowired
    protected RepositoryInitializer repositoryInitializer;
    @Autowired
    protected RepositoryObjectLoader repoObjLoader;
    @Autowired
    private JedisPool jedisPool;

    protected File depositsDirectory;
    protected File depositDir;
    protected String jobUUID;
    protected String depositUUID;
    protected PID depositPid;

    protected ContentRootObject rootObj;

    @BeforeEach
    public void initBase() throws Exception {
        TestHelper.setContentBase(baseAddress);

        depositsDirectory = tmpFolder.resolve("deposits").toFile();

        jobUUID = UUID.randomUUID().toString();

        depositPid = pidMinter.mintDepositRecordPid();
        depositUUID = depositPid.getId();
        depositDir = new File(depositsDirectory, depositUUID);
        depositDir.mkdir();

        depositStatusFactory.setState(depositUUID, DepositState.running);

        repositoryInitializer.initializeRepository();
        rootObj = repoObjLoader.getContentRootObject(RepositoryPaths.getContentRootPid());
    }

    @AfterEach
    public void cleanupDatastores() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        }
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
    }

    protected ContentObject findContentObjectByPid(List<ContentObject> objs, final PID pid) {
        return objs.stream()
                .filter(p -> p.getPid().equals(pid)).findAny().get();
    }

    protected boolean objectExists(PID pid) throws Exception {
        try (FcrepoResponse response = client.head(pid.getRepositoryUri())
                .perform()) {
            return true;
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw new FedoraException("Failed to check on object " + pid
                    + " during initialization", e);
        }
    }
}
