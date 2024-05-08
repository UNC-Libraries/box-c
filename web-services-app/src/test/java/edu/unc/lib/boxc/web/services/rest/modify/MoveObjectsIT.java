package edu.unc.lib.boxc.web.services.rest.modify;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.modify.MoveObjectsController.MoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/move-objects-it-servlet.xml")
})
public class MoveObjectsIT extends AbstractAPIIT {

    private AdminUnit unitObj;
    private CollectionObject collObj;
    private ContentContainerObject sourceContainer;
    private ContentContainerObject destContainer;

    @BeforeEach
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

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
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

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertNotNull(respMap.get("id"));

        assertObjectsAtDestination(movePids);

        assertObjectsRemovedFromSource(movePids, sourceContainer);

        assertTrue(sourceContainer.getMembers().stream().filter(m -> m.getPid().equals(stayPid)).findAny().isPresent(),
                "Unmoved object not present in source");
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

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
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

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
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
            assertTrue(destMembers.stream().filter(m -> m.getPid().equals(movePid)).findAny().isPresent(),
                    "Destination did not contain moved object");
        }
        assertEquals(movePids.size(), destMembers.size(), "Incorrect number of objects in container");
    }

    private void assertObjectsRemovedFromSource(List<PID> movePids, ContentContainerObject source) {
        List<ContentObject> sourceMembers = source.getMembers();
        for (PID movePid : movePids) {
            assertFalse(sourceMembers.stream().filter(m -> m.getPid().equals(movePid)).findAny().isPresent(),
                    "Source contained moved object");
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
