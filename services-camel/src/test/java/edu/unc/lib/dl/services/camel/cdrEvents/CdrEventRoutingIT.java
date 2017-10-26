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
package edu.unc.lib.dl.services.camel.cdrEvents;

import static edu.unc.lib.dl.util.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_STATUS;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.services.camel.solrUpdate.SolrUpdateProcessor;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/cdr-event-routing-it-context.xml"})
public class CdrEventRoutingIT {

    private static final String USER_ID = "user";
    private static final String DEPOSIT_ID = "deposit";
    private static final String BASE_URI = "http://example.com/rest/";

    @Autowired
    private OperationsMessageSender opsMsgSender;

    @Autowired
    private SolrUpdateProcessor solrUpdateProcessor;

    @Autowired
    private CamelContext cdrServiceSolrUpdate;

    @Mock
    private Map<IndexingActionType, IndexingAction> mockActionMap;
    @Mock
    private IndexingAction mockIndexingAction;
    @Captor
    private ArgumentCaptor<SolrUpdateRequest> updateRequestCaptor;

    @Before
    public void init() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(BASE_URI);

        solrUpdateProcessor.setSolrIndexingActionMap(mockActionMap);

        when(mockActionMap.get(any(IndexingActionType.class)))
                .thenReturn(mockIndexingAction);
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> added = pidList(3);
        List<PID> destinations = pidList(1);

        opsMsgSender.sendAddOperation(USER_ID, destinations, added, emptyList(), DEPOSIT_ID);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        notify.matches(3l, TimeUnit.SECONDS);

        verify(mockIndexingAction).performAction(updateRequestCaptor.capture());

        ChildSetRequest updateRequest = (ChildSetRequest) updateRequestCaptor.getValue();
        assertEquals(ADD_SET_TO_PARENT, updateRequest.getUpdateAction());

        assertTrue(updateRequest.getChildren().containsAll(added));
    }

    @Test
    public void testPublishAction() throws Exception {
        int numPids = 3;
        List<PID> pids = pidList(numPids);

        opsMsgSender.sendPublishOperation(USER_ID, pids, true);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(numPids)
                .create();

        notify.matches(3l, TimeUnit.SECONDS);

        verify(mockIndexingAction, times(numPids)).performAction(updateRequestCaptor.capture());

        // Verify that all requests processed triggered status updates
        List<SolrUpdateRequest> updateRequests = updateRequestCaptor.getAllValues();
        updateRequests.stream()
                .peek(r -> assertEquals(UPDATE_STATUS, r.getUpdateAction()));
    }

    private List<PID> pidList(int count) {
        List<PID> pidList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pidList.add(PIDs.get(UUID.randomUUID().toString()));
        }
        return pidList;
    }
}
