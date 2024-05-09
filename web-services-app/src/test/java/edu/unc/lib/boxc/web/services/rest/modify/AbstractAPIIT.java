package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.commons.io.FileUtils;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

/**
 *
 * @author harring
 *
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public abstract class AbstractAPIIT {
    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Autowired(required = false)
    protected String baseAddress;
    @Autowired
    protected WebApplicationContext context;
    @Autowired(required = false)
    protected AccessControlService aclService;
    @Autowired(required = false)
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired(required = false)
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired(required = false)
    protected PIDMinter pidMinter;
    @Autowired(required = false)
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired(required = false)
    protected RepositoryInitializer repoInitializer;
    @Autowired(required = false)
    protected FcrepoClient fcrepoClient;
    @Autowired(required = false)
    protected StorageLocationTestHelper storageLocationTestHelper;

    protected ContentRootObject contentRoot;

    protected MockMvc mvc;

    @BeforeEach
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        if (baseAddress != null) {
            TestHelper.setContentBase(baseAddress);
        }
        if (repoInitializer != null) {
            setupContentRoot();
        }

        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    public void tearDown() throws Exception {
        GroupsThreadStore.clearStore();
        if (fcrepoClient != null) {
            String containerString = URIUtil.join(FcrepoPaths.getBaseUri(), RepositoryPathConstants.CONTENT_BASE);
            try (var result = fcrepoClient.delete(URI.create(containerString)).perform()) {
                if (result.getStatusCode() != 204) {
                    throw new RuntimeException("Failed to delete content container");
                }
            }
            String tombstoneString = URIUtil.join(containerString, RepositoryPathConstants.FCR_TOMBSTONE);
            try (var result = fcrepoClient.delete(URI.create(tombstoneString)).perform()) {
                if (result.getStatusCode() != 204) {
                    throw new RuntimeException("Failed to delete content container tombstone");
                }
            }
        }
    }

    protected void setupContentRoot() {
        repoInitializer.initializeRepository();
        contentRoot = repositoryObjectLoader.getContentRootObject(getContentRootPid());
    }

    protected PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    protected Path createBinaryContent(String content) throws IOException {
        return createBinaryContent(content, "file", ".txt");
    }

    protected Path createBinaryContent(String content, String filename, String extension) throws IOException {
        Path contentPath = Files.createTempFile(storageLocationTestHelper.getFirstStorageLocationPath(), filename, extension);
        FileUtils.writeStringToFile(contentPath.toFile(), content, "UTF-8");
        return contentPath;
    }
}
