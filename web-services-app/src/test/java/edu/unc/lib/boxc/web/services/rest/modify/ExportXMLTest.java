package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.web.services.rest.MvcTestHelpers.getMapFromResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;
import jakarta.jms.TextMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author harring
 *
 */
@ContextConfiguration("/export-xml-it-servlet.xml")
public class ExportXMLTest extends AbstractAPIIT {
    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private jakarta.jms.Session mockSession;
    @Mock
    private TextMessage mockTextMessage;

    private AutoCloseable closeable;
    @Autowired
    private ExportXMLRequestService exportXmlRequestService;
    private PIDMinter pidMinter;
    private List<ExportXMLRequest> receivedMessages;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        pidMinter = new RepositoryPIDMinter();
        receivedMessages = new ArrayList<>();

        // When createTextMessage is called, capture the message content
        when(mockSession.createTextMessage(anyString())).thenAnswer(invocation -> {
            String messageText = invocation.getArgument(0);
            when(mockTextMessage.getText()).thenReturn(messageText);
            // Store the deserialized message for test assertions
            receivedMessages.add(exportXmlRequestService.deserializeRequest(messageText));
            return mockTextMessage;
        });

        // Configure mock JmsTemplate to capture messages
        doAnswer(invocation -> {
            var messageCreator = invocation.getArgument(0, MessageCreator.class);
            return messageCreator.createMessage(mockSession);
        }).when(jmsTemplate).send(any(MessageCreator.class));

        setField(exportXmlRequestService, "jmsTemplate", jmsTemplate);
    }

    @AfterEach
    public void shutdown() throws Exception {
        closeable.close();
    }

    @Test
    public void testExportMODS() throws Exception {
        List<String> exports = createObjects();
        String json = makeExportJson(exports, false);
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));

        Awaitility.await("Number of messages was " + receivedMessages.size())
                .atMost(Duration.ofSeconds(2)).until(() -> receivedMessages.size() == 1);
        ExportXMLRequest sentReq = receivedMessages.getFirst();
        assertEquals(USERNAME, sentReq.getAgent().getUsername());
        assertTrue(sentReq.getAgent().getPrincipals().containsAll(GROUPS));
        assertEquals(exports, sentReq.getPids());
        assertNotNull(sentReq.getRequestedTimestamp());
        assertFalse(sentReq.getExportChildren());
        assertNull(sentReq.getDatastreams());
    }

    @Test
    public void testExportChildren() throws Exception {
        List<String> exports = createObjects();
        String json = makeExportJson(exports, true);
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));

        Awaitility.await("Number of messages was " + receivedMessages.size()).atMost(Duration.ofSeconds(2)).until(() -> receivedMessages.size() == 1);
        ExportXMLRequest sentReq = receivedMessages.get(0);
        assertEquals(USERNAME, sentReq.getAgent().getUsername());
        assertTrue(sentReq.getAgent().getPrincipals().containsAll(GROUPS));
        assertEquals(exports, sentReq.getPids());
        assertNotNull(sentReq.getRequestedTimestamp());
        assertTrue(sentReq.getExportChildren());
        assertNull(sentReq.getDatastreams());
    }

    @Test
    public void testExportMultipleDatastreams() throws Exception {
        List<String> exports = createObjects();
        ExportXMLRequest export = new ExportXMLRequest();
        export.setPids(exports);
        export.setExportChildren(false);
        export.setEmail("user@example.com");
        export.setDatastreams(EnumSet.of(DatastreamType.TECHNICAL_METADATA, DatastreamType.MD_DESCRIPTIVE,
                DatastreamType.MD_DESCRIPTIVE_HISTORY));

        String json = exportXmlRequestService.serializeRequest(export);
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));

        Awaitility.await("Number of messages was " + receivedMessages.size())
                .atMost(Duration.ofSeconds(2)).until(() -> receivedMessages.size() == 1);
        ExportXMLRequest sentReq = receivedMessages.get(0);
        assertEquals(USERNAME, sentReq.getAgent().getUsername());
        assertTrue(sentReq.getAgent().getPrincipals().containsAll(GROUPS));
        assertEquals(exports, sentReq.getPids());
        assertNotNull(sentReq.getRequestedTimestamp());
        assertFalse(sentReq.getExportChildren());
        assertTrue(sentReq.getDatastreams().contains(DatastreamType.TECHNICAL_METADATA));
        assertTrue(sentReq.getDatastreams().contains(DatastreamType.MD_DESCRIPTIVE));
        assertTrue(sentReq.getDatastreams().contains(DatastreamType.MD_DESCRIPTIVE_HISTORY));
        assertEquals(3, sentReq.getDatastreams().size());
    }

    private List<String> createObjects() throws Exception {
        String pid1 = pidMinter.mintContentPid().getId();
        String pid2 = pidMinter.mintContentPid().getId();
        List<String> pids = new ArrayList<>();
        pids.add(pid1);
        pids.add(pid2);

        return pids;
    }

    private String makeExportJson(List<String> pids, boolean exportChildren) throws IOException {
        ExportXMLRequest export = new ExportXMLRequest();
        export.setPids(pids);
        export.setExportChildren(exportChildren);
        export.setEmail("user@example.com");
        return exportXmlRequestService.serializeRequest(export);
    }
}
