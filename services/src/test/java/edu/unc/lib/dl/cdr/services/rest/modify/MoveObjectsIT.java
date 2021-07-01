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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.dl.cdr.services.rest.modify.MoveObjectsController.MoveRequest;

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

    private AdminUnit unitObj;
    private CollectionObject collObj;
    private ContentContainerObject sourceContainer;
    private ContentContainerObject destContainer;

    @Before
    public void setup() {
        setupContentRoot();
        createHierarchy();
    }

    private void createHierarchy() {
        unitObj = repositoryObjectFactory.createAdminUnit(null);
        collObj = repositoryObjectFactory.createCollectionObject(null);
        sourceContainer = repositoryObjectFactory.createFolderObject(null);
        destContainer = repositoryObjectFactory.createFolderObject(null);

        contentRoot.addMember(unitObj);
        unitObj.addMember(collObj);
        collObj.addMember(destContainer);
        collObj.addMember(sourceContainer);
    }

    @Test
    public void testMoveOneObject() throws Exception {
        PID pid = makePid();
        List<PID> movePids = asList(pid);

        addSourceMembers(pid);

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

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

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

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

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

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

        MvcResult result = mvc.perform(post("/edit/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(movePids)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

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
}
