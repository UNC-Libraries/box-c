package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 *
 * @author harring
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"/spring-test/cdr-client-container.xml"})
public abstract class AbstractFedoraIT {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected FcrepoClient client;

    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected RepositoryObjectFactory repoObjFactory;
    @Autowired
    protected RepositoryObjectLoader repoObjLoader;
    @Autowired
    protected TransactionManager txManager;
    @Autowired
    protected RepositoryObjectDriver driver;
    @Autowired
    protected StorageLocationTestHelper storageLocationTestHelper;
    @Autowired
    protected FcrepoClient fcrepoClient;

    @Autowired
    protected Model queryModel;
    @Autowired
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    protected RepositoryInitializer repoInitializer;

    @BeforeEach
    public void init_() {
        // Override base uri for IT tests
        TestHelper.setContentBase(baseAddress);
    }

    @AfterEach
    public void cleanup() throws Exception {
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
    }

    protected URI createBaseContainer(String name) throws IOException, FcrepoOperationFailedException {

        URI baseUri = URI.create(URIUtil.join(baseAddress, name));
        // Create a parent object to put the binary into
        try (FcrepoResponse response = client.put(baseUri).perform()) {
            return response.getLocation();
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
                throw e;
            }
            // Ignore duplicate creation of base container
            return baseUri;
        }
    }

    /**
     * Asserts that the object identified by pid has been created in Fedora
     *
     * @param pid
     * @throws FcrepoOperationFailedException
     * @throws IOException
     */
    protected void assertObjectExists(PID pid) throws IOException, FcrepoOperationFailedException {
        assertObjectExists(pid.getRepositoryUri());
    }

    protected void assertObjectExists(URI uri) throws IOException, FcrepoOperationFailedException {
        try (FcrepoResponse response = client.head(uri).perform()) {
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }
    }

    protected ContentObject findContentObjectByPid(List<ContentObject> objs, final PID pid) {
        return objs.stream()
                .filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
