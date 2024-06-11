package edu.unc.lib.boxc.web.services.rest.modify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;

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

    private AdminUnit adminUnit;
    private CollectionObject collObj;

    @Autowired
    private JmsTemplate jmsTemplate;

    @BeforeEach
    public void setup() throws Exception {
        reset(jmsTemplate);

        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);

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
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(ADMIN_GROUP);
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
        CollectionObject collObj2 = repositoryObjectFactory.createCollectionObject(null);
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
        CollectionObject collObj2 = repositoryObjectFactory.createCollectionObject(null);
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
        assertTrue(resp.containsKey("job"), "Missing job id");
        assertEquals("destroy", resp.get("action"));
    }

    private void createCollectionInUnit(Model unitModel) {
        adminUnit = repositoryObjectFactory.createAdminUnit(unitModel);
        contentRoot.addMember(adminUnit);
        collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);
    }
}
