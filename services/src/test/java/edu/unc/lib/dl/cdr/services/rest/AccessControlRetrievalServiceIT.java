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
import static edu.unc.lib.dl.acl.util.UserRole.canAccess;
import static edu.unc.lib.dl.acl.util.UserRole.unitOwner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.UserRole;
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
import edu.unc.lib.dl.model.InvalidOperationForObjectType;
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
public class AccessControlRetrievalServiceIT extends AbstractAPIIT {
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
    private Map<String, Set<String>> objPrincRoles;

    @Before
    public void init_() throws Exception {
        generateBaseStructure();

        workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        String contentText = "Content";
        fileObj = workObj.addDataFile(new ByteArrayInputStream(contentText.getBytes()),
                "text.txt", "text/plain", null, null);

        workObj.setPrimaryObject(fileObj.getPid());
        workObj.addDescription(getClass().getResourceAsStream("/datastreams/simpleMods.xml"));
        uuid = workObj.getPid().getUUID();

        objPrincRoles = new HashMap<>();
        addPrincipalRoles(objPrincRoles, "admin", unitOwner);
        addPrincipalRoles(objPrincRoles, AUTHENTICATED_PRINC, canAccess);
    }

    @Test
    public void testGetPermissions() throws UnsupportedOperationException, Exception {
        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);

        MvcResult result = mvc.perform(get("/acl/getPermissions/" + uuid))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);

        assertTrue(respMap.containsKey("memberPermissions"));
        assertEquals(uuid, respMap.get("uuid"));
        assertEquals(false, respMap.get("markForDeletion"));
        assertEquals(null, respMap.get("embargoed"));
        assertEquals(PatronAccess.parent.toString(), respMap.get("patronAccess"));
    }

    @Test(expected = InvalidOperationForObjectType.class)
    public void testGetPermissionsWrongObjType() throws UnsupportedOperationException, Exception {
        RepositoryObject wrongObjType = repositoryObjectLoader.getPremisEventObject(fileObj.getPid());

        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj, wrongObjType);

        mvc.perform(get("/acl/getPermissions/" + wrongObjType.getPid().getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMemberPermissions() throws UnsupportedOperationException, Exception {
        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);

        MvcResult result = mvc.perform(get("/acl/getPermissions/" + uuid))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);

        Map<String,Object> returnedValues = ((ArrayList<Map<String, Object>>) respMap.get("memberPermissions")).get(0);
        Object principals = returnedValues.get("principals");
        assertTrue(respMap.containsKey("memberPermissions"));
        assertEquals(fileObj.getPid().getUUID(), returnedValues.get("uuid"));
    //    assertEquals(objPrincRoles, principals);

        assertTrue(((Map<String, Object>)  principals).containsKey("admin"));
    //    assertTrue(((Map<String, Object>)  principals).containsValue(unitOwner));
        assertTrue(((Map<String, Object>)  principals).containsKey("authenticated"));
   //     assertTrue(((Map<String, Object>)  principals).containsValue(canAccess));

        assertEquals(false, returnedValues.get("markForDeletion"));
        assertEquals(null, returnedValues.get("embargoed"));
        assertEquals(PatronAccess.parent.toString(), returnedValues.get("patronAccess"));
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

    private void addPrincipalRoles(Map<String, Set<String>> objPrincRoles,
            String princ, UserRole... roles) {
        Set<String> roleSet = Arrays.stream(roles)
            .map(r -> r.getPropertyString())
            .collect(Collectors.toSet());
        objPrincRoles.put(princ, roleSet);
    }
}
