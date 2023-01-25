package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class CdrEventToSolrUpdateProcessorTest {
    private static final int NUM_TEST_PIDS = 3;
    private static final String USER_ID = "user_id";

    private CdrEventToSolrUpdateProcessor processor;

    @Mock
    private IndexingMessageSender messageSender;
    @Mock
    private Exchange exchange;
    @Mock
    private Message msg;

    @Captor
    ArgumentCaptor<PID> pidCaptor;
    @Captor
    ArgumentCaptor<Collection<PID>> pidsCaptor;
    @Captor
    ArgumentCaptor<String> stringCaptor;
    @Captor
    ArgumentCaptor<IndexingActionType> actionTypeCaptor;

    private PID targetPid;

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);

        processor = new CdrEventToSolrUpdateProcessor();
        processor.setIndexingMessageSender(messageSender);

        targetPid = PIDs.get(UUID.randomUUID().toString());

        when(exchange.getIn()).thenReturn(msg);
        when(msg.getHeader(eq("name"))).thenReturn(USER_ID);
    }

    @Test
    public void testNoMessageBody() throws Exception {
        when(msg.getBody()).thenReturn(null);

        processor.process(exchange);

        verify(messageSender, never()).sendIndexingOperation(anyString(), any(PID.class),
                any(IndexingActionType.class));
    }

    @Test
    public void testUnknownAction() throws Exception {
        List<PID> subjects = pidList(1);

        Document msgDoc = buildMessage("unknown", "unknown", targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(messageSender, never()).sendIndexingOperation(anyString(), any(PID.class),
                anyCollectionOf(PID.class), any(IndexingActionType.class));
    }

    @Test
    public void testMoveAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(CDRActions.MOVE.toString(), CDRActions.MOVE.getName(), targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(messageSender).sendIndexingOperation(stringCaptor.capture(), pidCaptor.capture(), pidsCaptor.capture(),
                actionTypeCaptor.capture(), isNull(), isNull());

        verifyTargetPid(pidCaptor.getValue());

        verifyUserid(stringCaptor.getValue());

        verifyChildPids(subjects);

        verifyActionType(IndexingActionType.MOVE, actionTypeCaptor.getValue());
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(CDRActions.ADD.toString(), CDRActions.ADD.getName(), targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(messageSender).sendIndexingOperation(stringCaptor.capture(), pidCaptor.capture(), pidsCaptor.capture(),
                actionTypeCaptor.capture(), isNull(), isNull());

        verifyTargetPid(pidCaptor.getValue());

        verifyUserid(stringCaptor.getValue());

        verifyChildPids(subjects);

        verifyActionType(IndexingActionType.ADD_SET_TO_PARENT, actionTypeCaptor.getValue());
    }

    private Document buildMessage(String operation, String contentName, PID pid, List<PID> subjects) {
        when(msg.getHeader(eq(CdrUpdateAction))).thenReturn(operation);

        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("title", ATOM_NS)
                .setText(operation).setAttribute("type", "text"));
        entry.addContent(new Element("summary", ATOM_NS)
                .setText(pid.getRepositoryPath()).setAttribute("type", "text"));
        Element content = new Element("content", ATOM_NS).setAttribute("type", "text/xml");
        entry.addContent(content);
        Element contentBody = new Element(contentName, CDR_MESSAGE_NS);
        content.addContent(contentBody);

        if (subjects != null && subjects.size() > 0) {
            addPidListElement(contentBody, "subjects", subjects);
        }

        return msg;
    }

    private void addPidListElement(Element parentEl, String elementName, List<PID> pids) {
        if (pids != null && pids.size() > 0) {
            Element listEl = new Element("subjects", CDR_MESSAGE_NS);
            parentEl.addContent(listEl);
            for (PID pid : pids) {
                listEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(pid.getId()));
            }
        }
    }

    private List<PID> pidList(int count) {
        List<PID> pidList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pidList.add(PIDs.get(UUID.randomUUID().toString()));
        }
        return pidList;
    }

    private void verifyTargetPid(PID pid) {
        assertEquals(targetPid, pid);
    }

    private void verifyChildPids(List<PID> subjects) {
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(NUM_TEST_PIDS, pids.size());
        assertTrue(pids.containsAll(subjects));
    }

    private void verifyUserid(String userid) {
        assertEquals(USER_ID, userid);
    }

    private void verifyActionType(IndexingActionType expected, IndexingActionType actual) {
        assertEquals(expected, actual);
    }
}
