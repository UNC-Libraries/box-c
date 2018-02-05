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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.cdr.services.rest.modify.MoveObjectsController.MoveRequest;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/move-objects-it-servlet.xml")
})
public class MoveObjectsIT extends AbstractAPIIT {

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private Model queryModel;
    @Autowired
    private FcrepoClient fcrepoClient;

    private ContentRootObject rootObj;
    private AdminUnit unitObj;
    private CollectionObject collObj;
    private ContentContainerObject sourceContainer;
    private ContentContainerObject destContainer;

    @Before
    public void setup() {
        createHierarchy();
    }

    private void createHierarchy() {
        PID rootPid = RepositoryPaths.getContentRootPid();
        try {
            repositoryObjectFactory.createContentRootObject(rootPid.getRepositoryUri(), null);
        } catch (FedoraException e) {
        }
        rootObj = repositoryObjectLoader.getContentRootObject(rootPid);

        unitObj = repositoryObjectFactory.createAdminUnit(null);
        collObj = repositoryObjectFactory.createCollectionObject(null);
        sourceContainer = repositoryObjectFactory.createFolderObject(null);
        destContainer = repositoryObjectFactory.createFolderObject(null);

        rootObj.addMember(unitObj);
        unitObj.addMember(collObj);
        collObj.addMember(destContainer);
        collObj.addMember(sourceContainer);
    }

    @Test
    public void testMoveOneObject() throws Exception {
        PID pid = makePid();
        List<PID> movePids = asList(pid);

        addSourceMembers(pid);

        indexAll();

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertNotNull(respMap.get("id"));

        assertObjectsAtDestination(movePids);

        assertObjectsRemovedFromSource(movePids, sourceContainer);
    }

    @Test
    public void testMoveSubsetFromSource() throws Exception {
        PID movePid = makePid();
        List<PID> movePids = asList(movePid);
        PID stayPid = makePid();

        addSourceMembers(movePid, stayPid);

        indexAll();

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertNotNull(respMap.get("id"));

        assertObjectsAtDestination(movePids);

        assertObjectsRemovedFromSource(movePids, sourceContainer);

        assertTrue("Unmoved object not present in source", sourceContainer.getMembers().stream().
                filter(m -> m.getPid().equals(stayPid)).findAny().isPresent());
    }

    @Test
    public void testMoveMultipleFromSource() throws Exception {
        PID pid1 = makePid();
        PID pid2 = makePid();
        List<PID> movePids = asList(pid1, pid2);

        addSourceMembers(pid1, pid2);

        indexAll();

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertNotNull(respMap.get("id"));

        assertObjectsAtDestination(movePids);

        assertObjectsRemovedFromSource(movePids, sourceContainer);
    }

    @Test
    public void testMoveFromMultipleSources() throws Exception {
        PID pid1 = makePid();
        addSourceMembers(pid1);

        FolderObject sourceContainer2 = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(sourceContainer2);
        PID pid2 = makePid();
        addSourceMembers(sourceContainer2, pid2);

        List<PID> movePids = asList(pid1, pid2);

        indexAll();

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertNotNull(respMap.get("id"));

        assertObjectsAtDestination(movePids);

        assertObjectsRemovedFromSource(asList(pid1), sourceContainer);
        assertObjectsRemovedFromSource(asList(pid2), sourceContainer2);
    }

    private void addSourceMembers(PID... pids) {
        addSourceMembers(sourceContainer, pids);
    }

    private void addSourceMembers(ContentContainerObject source, PID... pids) {
        for (PID pid : pids) {
            ContentObject memberObj = repositoryObjectFactory.createWorkObject(pid, null);
            source.addMember(memberObj);
        }
    }

    private void assertObjectsAtDestination(List<PID> movePids) {
        destContainer.setEtag(null);
        List<ContentObject> destMembers = destContainer.getMembers();
        for (PID movePid : movePids) {
            assertTrue("Destination did not contain moved object", destMembers.stream().
                    filter(m -> m.getPid().equals(movePid)).findAny().isPresent());
        }
        assertEquals("Incorrect number of objects in container", movePids.size(), destMembers.size());
    }

    private void assertObjectsRemovedFromSource(List<PID> movePids, ContentContainerObject source) {
        sourceContainer.setEtag(null);
        List<ContentObject> sourceMembers = source.getMembers();
        for (PID movePid : movePids) {
            assertFalse("Source contained moved object", sourceMembers.stream().
                    filter(m -> m.getPid().equals(movePid)).findAny().isPresent());
        }
    }

    private byte[] makeRequestBody(List<PID> movePids) throws Exception {
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setDestination(destContainer.getPid().getId());
        List<String> pidStrings = movePids.stream().map(p -> p.getId()).collect(Collectors.toList());
        moveRequest.setMoved(pidStrings);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(moveRequest);
    }

    private void indexAll() throws Exception {
        queryModel.removeAll();

        indexTree(rootObj.getModel());
    }

    private void indexTree(Model model) throws Exception {
        queryModel.add(model);

        indexRelated(model, Ldp.contains);
        indexRelated(model, PcdmModels.hasMember);
    }

    private void indexRelated(Model model, Property relationProp) throws Exception {
        NodeIterator containedIt = model.listObjectsOfProperty(relationProp);
        while (containedIt.hasNext()) {
            RDFNode contained = containedIt.next();
            URI rescUri = URI.create(contained.asResource().getURI());
            try (FcrepoResponse resp = fcrepoClient.get(rescUri).perform()) {
                Model childModel = ModelFactory.createDefaultModel();
                childModel.read(resp.getBody(), null, Lang.TURTLE.getName());
                indexTree(childModel);
            }
        }
    }
}
