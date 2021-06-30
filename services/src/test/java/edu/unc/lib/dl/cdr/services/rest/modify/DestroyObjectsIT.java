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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.CollectionObjectImpl;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.test.AclModelBuilder;

/**
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/destroy-objects-it-servlet.xml")
})
public class DestroyObjectsIT extends AbstractAPIIT {
    private static final String USER_NAME = "user";
    private static final String USER_GROUPS = "edu:lib:staff_grp";
    private static final String ADMIN_GROUP = "adminGroup";

    private AdminUnitImpl adminUnit;
    private CollectionObjectImpl collObj;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Before
    public void setup() throws Exception {
        reset(jmsTemplate);

        AccessGroupSet testPrincipals = new AccessGroupSet(USER_GROUPS);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);

        setupContentRoot();
    }

    @Test
    public void destroySingle() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Unit with owner")
                    .addUnitOwner(USER_GROUPS)
                    .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(post("/edit/destroy/" + collObj.getPid().getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertResponseSuccess(result);

        verify(jmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void destroyUnitAsManagerForbidden() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Unit with manager")
                .addCanManage(USER_GROUPS)
                .model);

        treeIndexer.indexAll(baseAddress);

        mvc.perform(post(URI.create("/edit/destroy/" + adminUnit.getPid().getId())))
                .andExpect(status().isForbidden())
                .andReturn();

        verify(jmsTemplate, never()).send(any(MessageCreator.class));
    }

    @Test
    public void destroyUnitAsAdmin() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(ADMIN_GROUP);
        GroupsThreadStore.storeGroups(testPrincipals);

        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        mvc.perform(post(URI.create("/edit/destroy/" + adminUnit.getPid().getId())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(jmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void destroyMultiple() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Unit with owner")
                .addUnitOwner(USER_GROUPS)
                .model);
        CollectionObjectImpl collObj2 = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj2);

        treeIndexer.indexAll(baseAddress);

        String ids = collObj.getPid().getId() + "\n" + collObj2.getPid().getId();

        MvcResult result = mvc.perform(post("/edit/destroy")
                .param("ids", ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertResponseSuccess(result);

        verify(jmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void destroyMultipleNoPermission() throws Exception {
        createCollectionInUnit(null);
        CollectionObjectImpl collObj2 = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj2);

        treeIndexer.indexAll(baseAddress);

        String ids = collObj.getPid().getId() + "\n" + collObj2.getPid().getId();

        mvc.perform(post("/edit/destroy")
                .param("ids", ids))
                .andExpect(status().isForbidden())
                .andReturn();

        verify(jmsTemplate, never()).send(any(MessageCreator.class));
    }

    @Test
    public void destroyNone() throws Exception {
        mvc.perform(post("/edit/destroy")
                .param("ids", ""))
                .andExpect(status().isBadRequest())
                .andReturn();

        verify(jmsTemplate, never()).send(any(MessageCreator.class));
    }

    private void assertResponseSuccess(MvcResult mvcResult) throws Exception {
        Map<String, Object> resp = getMapFromResponse(mvcResult);
        assertTrue("Missing job id", resp.containsKey("job"));
        assertEquals("destroy", resp.get("action"));
    }

    private void createCollectionInUnit(Model unitModel) {
        adminUnit = repositoryObjectFactory.createAdminUnit(unitModel);
        contentRoot.addMember(adminUnit);
        collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);
    }
}
