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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;

/**
 *
 * @author lfarrell
 * @author harring
 *
 */

@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/access-control-retrieval-it-servlet.xml")
})
public class AccessControlRetrievalIT extends AbstractAPIIT {
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private Model queryModel;
    @Autowired
    private RepositoryPIDMinter pidMinter;

    private ContentRootObject rootObj;
    private AdminUnit unitObj;
    private CollectionObject collObj;
    private WorkObject workObj;
    private FileObject fileObj;
    private String uuid;

    @Before
    public void init_() throws Exception {
        generateBaseStructure();

        workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        String contentText = "Content";
        fileObj = workObj.addDataFile(new ByteArrayInputStream(contentText.getBytes()),
                "text.txt", "text/plain", null, null);

        workObj.setPrimaryObject(fileObj.getPid());
        uuid = workObj.getPid().getId();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetPermissionsObjectWithChildren() throws Exception {
        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);

        MvcResult result = mvc.perform(get("/acl/getPermissions/" + uuid))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // check first-level response fields
        Map<String,Object> respMap = getMapFromResponse(result);
        assertRespMapHasRequiredFields(respMap, uuid);

        // check access controls of parent
        Map<String,Object> aclMap = (Map<String, Object>) respMap.get("accessControls");
        assertHasCorrectAccessControls(aclMap, uuid);
        assertTrue(aclMap.containsKey("memberPermissions"));

        // check access controls of child
        List<Map<String, Object>> memberPermissions = (List<Map<String, Object>>) aclMap.get("memberPermissions");
        Map<String, Object> memPermsMap = memberPermissions.get(0);
        assertHasCorrectAccessControls(memPermsMap, fileObj.getPid().getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetPermissionsObjectWithoutChildren() throws Exception {
        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);
        String fileUuid = fileObj.getPid().getId();

        MvcResult result = mvc.perform(get("/acl/getPermissions/" + fileUuid))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String,Object> respMap = getMapFromResponse(result);
        assertRespMapHasRequiredFields(respMap, fileUuid);

        Map<String,Object> aclMap = (Map<String, Object>) respMap.get("accessControls");
        assertHasCorrectAccessControls(aclMap, fileUuid);
        assertFalse(aclMap.containsKey("memberPermissions"));
    }

    private void assertRespMapHasRequiredFields(Map<String, Object> respMap, String uuid) {
        assertEquals("retrieve acls", respMap.get("action"));
        assertEquals(uuid, respMap.get("pid"));
        assertTrue(respMap.containsKey("accessControls"));
        assertTrue(respMap.containsKey("timestamp"));
    }

    @SuppressWarnings("unchecked")
    private void assertHasCorrectAccessControls(Map<String, Object> aclMap, String uuid) {
        assertEquals(uuid, aclMap.get("pid"));
        assertEquals(false, aclMap.get("markForDeletion"));
        assertNull(aclMap.get("inheritedEmbargo"));
        assertNull(aclMap.get("embargo"));
        assertEquals("parent", aclMap.get("patronAccess"));
        assertEquals("parent", aclMap.get("inheritedPatronAccess"));
        Map<String, Set<String>> principals = (Map<String, Set<String>>) aclMap.get("principals");
        assertEquals(0, principals.size());
        Map<String, Set<String>> inheritedPrincipals = (Map<String, Set<String>>) aclMap.get("inheritedPrincipals");
        assertEquals(2, inheritedPrincipals.size());
        assertTrue(inheritedPrincipals.containsKey("admin"));
        assertTrue(inheritedPrincipals.containsKey("authenticated"));
    }

    private void indexObjectsInTripleStore(RepositoryObject... objs) {
        for (RepositoryObject obj : objs) {
            queryModel.add(obj.getModel());
        }
    }

    private void generateBaseStructure() throws Exception {
        PID rootPid = pidMinter.mintContentPid();
        repositoryObjectFactory.createContentRootObject(rootPid.getRepositoryUri(), null);
        rootObj = repositoryObjectLoader.getContentRootObject(rootPid);

        PID unitPid = pidMinter.mintContentPid();
        Model unitModel = ModelFactory.createDefaultModel();
        Resource unitResc = unitModel.getResource(unitPid.getRepositoryPath());
        unitResc.addProperty(CdrAcl.unitOwner, "admin");
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid, unitModel);
        rootObj.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        Model collModel = ModelFactory.createDefaultModel();
        Resource collResc = collModel.getResource(collPid.getRepositoryPath());
        collResc.addProperty(CdrAcl.canAccess, AUTHENTICATED_PRINC);
        collObj = repositoryObjectFactory.createCollectionObject(collPid, collModel);
        unitObj.addMember(collObj);
    }

}
