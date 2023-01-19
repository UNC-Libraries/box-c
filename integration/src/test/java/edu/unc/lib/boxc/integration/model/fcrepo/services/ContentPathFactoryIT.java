package edu.unc.lib.boxc.integration.model.fcrepo.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.integration.fcrepo.AbstractFedoraIT;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

/**
 *
 * @author bbpennel
 *
 */
public class ContentPathFactoryIT extends AbstractFedoraIT {

    @Autowired
    private ContentPathFactory pathFactory;

    private PID contentRootPid;
    private ContentRootObject contentRoot;
    private AdminUnit adminUnit;
    private CollectionObject collObj;

    @BeforeEach
    public void init() {
        contentRootPid = RepositoryPaths.getContentRootPid();
        repoInitializer.initializeRepository();
        contentRoot = repoObjLoader.getContentRootObject(contentRootPid);

        adminUnit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        collObj = repoObjFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);
    }

    @Test
    public void testGetAncestorPids() throws Exception {
        List<PID> ancestors = pathFactory.getAncestorPids(collObj.getPid());

        assertEquals(2, ancestors.size(), "Incorrect number of ancestors");
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));
    }

    @Test
    public void testObjectNotFound() throws Exception {
        Assertions.assertThrows(NotFoundException.class, () -> {
            PID pid = pidMinter.mintContentPid();

            pathFactory.getAncestorPids(pid);
        });
    }

    @Test
    public void testGetFileAncestors() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(null);
        collObj.addMember(work);

        Path contentPath = Files.createTempFile("test", ".txt");
        FileObject fileObj = work.addDataFile(contentPath.toUri(), "file", "text/plain", null, null);
        BinaryObject binObj = fileObj.getOriginalFile();

        List<PID> ancestors = pathFactory.getAncestorPids(binObj.getPid());

        assertEquals(5, ancestors.size(), "Incorrect number of ancestors");
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));
        assertEquals(collObj.getPid(), ancestors.get(2));
        assertEquals(work.getPid(), ancestors.get(3));
        assertEquals(fileObj.getPid(), ancestors.get(4));
    }

    @Test
    public void testGetCachedAncestorPidsTest() throws Exception {
        treeIndexer.indexAll(baseAddress);

        List<PID> ancestors = pathFactory.getAncestorPids(collObj.getPid());

        assertEquals(2, ancestors.size(), "Incorrect number of ancestors");
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));

        // Switch ownership of coll to a new unit
        AdminUnit adminUnit2 = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit2);

        adminUnit2.addMember(collObj);

        // Wait for cache to expire and then check that the new path is retrieved
        TimeUnit.MILLISECONDS.sleep(200);

        ancestors = pathFactory.getAncestorPids(collObj.getPid());
        assertEquals(2, ancestors.size(), "Incorrect number of ancestors");
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit2.getPid(), ancestors.get(1), "Ancestors did not update to new unit");
    }
}
