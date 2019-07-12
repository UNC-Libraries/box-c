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
package edu.unc.lib.dl.fedora;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fcrepo4.WorkObject;

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
        try {
            repoObjFactory.createContentRootObject(
                    contentRootPid.getRepositoryUri(), null);
        } catch (FedoraException e) {
            // Ignore failure as the content root will already exist after first test
        }
        contentRoot = repoObjLoader.getContentRootObject(contentRootPid);

        adminUnit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        collObj = repoObjFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);
    }

    @Test
    public void testGetAncestorPids() throws Exception {
        treeIndexer.indexAll(baseAddress);

        List<PID> ancestors = pathFactory.getAncestorPids(collObj.getPid());

        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals(adminUnit.getPid(), ancestors.get(1));
    }

    @Test
    public void testGetFileAncestors() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(null);
        collObj.addMember(work);

        InputStream contentStream = new ByteArrayInputStream("content".getBytes());
        FileObject fileObj = work.addDataFile(contentStream, "file", "text/plain", null, null);
        BinaryObject binObj = fileObj.getOriginalFile();

        treeIndexer.indexAll(baseAddress);

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

        treeIndexer.indexAll(baseAddress);

        // Wait for cache to expire and then check that the new path is retrieved
        TimeUnit.MILLISECONDS.sleep(200);

        ancestors = pathFactory.getAncestorPids(collObj.getPid());
        assertEquals("Incorrect number of ancestors", 2, ancestors.size());
        assertEquals(contentRootPid, ancestors.get(0));
        assertEquals("Ancestors did not update to new unit", adminUnit2.getPid(), ancestors.get(1));
    }
}
