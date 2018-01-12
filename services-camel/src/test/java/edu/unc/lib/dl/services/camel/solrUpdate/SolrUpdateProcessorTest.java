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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static edu.unc.lib.dl.util.IndexingActionType.ADD;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class SolrUpdateProcessorTest {

    private SolrUpdateProcessor processor;

    private Map<IndexingActionType, IndexingAction> indexingActionMap;

    private Document bodyDoc;
    private PID targetPid;

    @Mock
    private Exchange exchange;
    @Mock
    private Message msg;

    @Mock
    private IndexingAction mockAddAction;

    @Captor
    private ArgumentCaptor<ChildSetRequest> childSetCaptor;

    @Before
    public void init() {
        initMocks(this);

        indexingActionMap = new HashMap<>();
        indexingActionMap.put(ADD, mockAddAction);

        processor = new SolrUpdateProcessor();
        processor.setSolrIndexingActionMap(indexingActionMap);

        bodyDoc = new Document();
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getBody()).thenReturn(bodyDoc);

        targetPid = makePid();
    }

    @Test
    public void testMessageNoChildren() throws Exception {
        populateEntry(ADD);

        processor.process(exchange);

        verify(mockAddAction).performAction(any(SolrUpdateRequest.class));
    }

    @Test
    public void testMessageWithChildren() throws Exception {
        populateEntry(ADD);
        List<PID> childrenPaths = addChildren(3);

        processor.process(exchange);

        verify(mockAddAction).performAction(childSetCaptor.capture());
        ChildSetRequest childSetRequest = childSetCaptor.getValue();

        assertTrue(childSetRequest.getChildren().containsAll(childrenPaths));
    }

    @Test
    public void testInvalidIndexingAction() throws Exception {
        // This action is not mapped
        populateEntry(DELETE);

        processor.process(exchange);

        verify(mockAddAction, never()).performAction(any());
    }

    private Element populateEntry(IndexingActionType type) {
        Element entry = new Element("entry", ATOM_NS);
        bodyDoc.addContent(entry);

        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getRepositoryPath()));

        entry.addContent(new Element("solrActionType", ATOM_NS)
                .setText(type.getURI().toString()));

        return entry;
    }

    private List<PID> addChildren(int count) {
        Element entry = bodyDoc.getRootElement();
        Element children = new Element("children", CDR_MESSAGE_NS);
        entry.addContent(children);

        List<PID> pids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PID pid = makePid();
            pids.add(pid);
            children.addContent(new Element("pid", CDR_MESSAGE_NS)
                    .setText(pid.getRepositoryPath()));
        }
        return pids;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
