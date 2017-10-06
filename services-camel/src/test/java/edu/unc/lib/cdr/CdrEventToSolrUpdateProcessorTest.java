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
package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;
import static edu.unc.lib.dl.util.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.dl.util.IndexingActionType.SET_DEFAULT_WEB_OBJECT;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_STATUS;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.ADD;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.MOVE;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

/**
 *
 * @author bbpennel
 *
 */
public class CdrEventToSolrUpdateProcessorTest {
    private static final int NUM_TEST_PIDS = 3;

    private CdrEventToSolrUpdateProcessor processor;

    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private Exchange exchange;
    @Mock
    private Message msg;

    @Captor
    ArgumentCaptor<MessageCreator> messageCreatorCaptor;
    @Captor
    ArgumentCaptor<String> stringCaptor;

    private PID targetPid;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new CdrEventToSolrUpdateProcessor();
        processor.setJmsTemplate(jmsTemplate);

        targetPid = PIDs.get(UUID.randomUUID().toString());

        when(exchange.getIn()).thenReturn(msg);
    }

    @Test
    public void testNoMessageBody() throws Exception {
        when(msg.getBody()).thenReturn(null);

        processor.process(exchange);

        verify(jmsTemplate, never()).send(any(MessageCreator.class));
    }

    @Test
    public void testUnknownAction() throws Exception {
        List<PID> subjects = pidList(1);

        Document msgDoc = buildMessage("unknown", "unknown", targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(jmsTemplate, never()).send(any(MessageCreator.class));
    }

    @Test
    public void testMoveAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(MOVE.toString(), MOVE.getName(), targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        Element sentMsg = getSentMessage();
        String solrAction = sentMsg.getChildText("solrActionType", ATOM_NS);
        assertEquals(IndexingActionType.MOVE.getURI().toString(), solrAction);

        assertAllPidsPresent(sentMsg, subjects);
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(ADD.toString(), ADD.getName(), targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        Element sentMsg = getSentMessage();
        String solrAction = sentMsg.getChildText("solrActionType", ATOM_NS);
        assertEquals(ADD_SET_TO_PARENT.getURI().toString(), solrAction);

        assertAllPidsPresent(sentMsg, subjects);
    }

    @Test
    public void testCleanIndexAction() throws Exception {
        testIndexingAction(IndexingActionType.CLEAN_REINDEX);
    }

    @Test
    public void testInplaceIndexAction() throws Exception {
        testIndexingAction(IndexingActionType.RECURSIVE_REINDEX);
    }

    private void testIndexingAction(IndexingActionType indexingAction) throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(CDRActions.INDEX.toString(),
                indexingAction.getName(),
                targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(jmsTemplate, times(NUM_TEST_PIDS)).send(messageCreatorCaptor.capture());
        List<MessageCreator> creators = messageCreatorCaptor.getAllValues();

        List<String> responsePids = new ArrayList<>();
        for (MessageCreator creator : creators) {
            Element sentMsg = getCreatedMessage(creator);
            String solrAction = sentMsg.getChildText("solrActionType", ATOM_NS);
            assertEquals(indexingAction.getURI().toString(), solrAction);

            String pid = sentMsg.getChildText("pid", ATOM_NS);
            responsePids.add(pid);
        }

        for (PID subject : subjects) {
            assertTrue(responsePids.stream().anyMatch(p -> p.equals(subject.getId())));
        }
    }

    @Test
    public void testIndexPrimaryObjectAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(CDRActions.INDEX.toString(),
                IndexingActionType.SET_DEFAULT_WEB_OBJECT.getName(),
                targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        Element sentMsg = getSentMessage();
        String solrAction = sentMsg.getChildText("solrActionType", ATOM_NS);
        assertEquals(SET_DEFAULT_WEB_OBJECT.getURI().toString(), solrAction);

        assertAllPidsPresent(sentMsg, subjects);
    }

    @Test
    public void testPublishAction() throws Exception {
        List<PID> subjects = pidList(NUM_TEST_PIDS);

        Document msgDoc = buildMessage(CDRActions.PUBLISH.toString(),
                UPDATE_STATUS.getName(),
                targetPid, subjects);
        when(msg.getBody()).thenReturn(msgDoc);

        processor.process(exchange);

        verify(jmsTemplate, times(NUM_TEST_PIDS)).send(messageCreatorCaptor.capture());
        List<MessageCreator> creators = messageCreatorCaptor.getAllValues();

        List<String> responsePids = new ArrayList<>();
        for (MessageCreator creator : creators) {
            Element sentMsg = getCreatedMessage(creator);
            String solrAction = sentMsg.getChildText("solrActionType", ATOM_NS);
            assertEquals(UPDATE_STATUS.getURI().toString(), solrAction);

            String pid = sentMsg.getChildText("pid", ATOM_NS);
            responsePids.add(pid);
        }

        for (PID subject : subjects) {
            assertTrue(responsePids.stream().anyMatch(p -> p.equals(subject.getId())));
        }
    }

    private Document buildMessage(String operation, String contentName, PID pid, List<PID> subjects) {
        when(msg.getHeader(eq(CdrSolrUpdateAction))).thenReturn(operation);

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

    private Element getSentMessage() throws Exception {
        verify(jmsTemplate).send(messageCreatorCaptor.capture());

        return getCreatedMessage(messageCreatorCaptor.getValue());
    }

    private Element getCreatedMessage(MessageCreator creator) throws Exception {
        Session session = mock(Session.class);
        creator.createMessage(session);
        verify(session).createTextMessage(stringCaptor.capture());

        SAXBuilder parser = new SAXBuilder();
        return parser.build(new ByteArrayInputStream(stringCaptor.getValue().getBytes()))
                .getRootElement();
    }

    private void assertAllPidsPresent(Element sentMsg, List<PID> pids) {
        String childText = sentMsg.getChildText("children", ATOM_NS);
        for (String child : childText.split(",")) {
            assertTrue(pids.stream().anyMatch(p -> p.getId().equals(child)));
        }
    }
}
