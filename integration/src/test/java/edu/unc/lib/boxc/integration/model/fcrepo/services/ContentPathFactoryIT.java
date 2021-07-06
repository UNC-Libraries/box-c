/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.integration.model.fcrepo.services;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
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

    @Before
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

        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));
    }

    @Test(expected = NotFoundException.class)
    public void testObjectNotFound() throws Exception {
        PID pid = pidMinter.mintContentPid();

        pathFactory.getAncestorPids(pid);
    }

    @Test
    public void testGetFileAncestors() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(null);
        collObj.addMember(work);

        Path contentPath = Files.createTempFile("test", ".txt");
        FileObject fileObj = work.addDataFile(contentPath.toUri(), "file", "text/plain", null, null);
        BinaryObject binObj = fileObj.getOriginalFile();

        List<PID> ancestors = pathFactory.getAncestorPids(binObj.getPid());

        assertEquals("Incorrect number of ancestors", 5, ancestors.size());
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

        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));

        // Switch ownership of coll to a new unit
        AdminUnit adminUnit2 = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit2);

        adminUnit2.addMember(collObj);

        // Wait for cache to expire and then check that the new path is retrieved
        TimeUnit.MILLISECONDS.sleep(200);

        ancestors = pathFactory.getAncestorPids(collObj.getPid());
        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals("Ancestors did not update to new unit", adminUnit2.getPid(), ancestors.get(1));
    }
}
