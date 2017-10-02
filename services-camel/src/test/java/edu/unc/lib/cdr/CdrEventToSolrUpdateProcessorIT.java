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

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/service-context.xml", "/cdr-event-to-solr-it-context.xml"})
public class CdrEventToSolrUpdateProcessorIT {

    private static final String USER_ID = "user";
    private static final String DEPOSIT_ID = "deposit";
    private static final String BASE_URI = "http://example.com/rest";

    @Autowired
    private OperationsMessageSender opsMsgSender;

    @Autowired
    private SolrUpdateProcessor solrUpdateProcessor;

    @Autowired
    private JmsTemplate cdrEventsJmsTemplate;

    @Autowired
    private Repository repository;

    private List<PID> destinations;

    @Before
    public void init() throws Exception {
        //initMocks(this);

        when(repository.getBaseUri()).thenReturn(BASE_URI);
        PIDs.setRepository(repository);

        destinations = pidList(1);
    }

    @Test
    public void testNoMessageBody() throws Exception {

    }

    @Test
    public void testUnknownAction() throws Exception {

    }

    @Test
    public void testMoveAction() throws Exception {
        // opsMsgSender.sendMoveOperation(userid, sources, destination, moved, reordered);
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> added = pidList(3);

        opsMsgSender.sendAddOperation(USER_ID, destinations, added, emptyList(), DEPOSIT_ID);

        Thread.sleep(5000);
        System.out.println("Finishing up");
//        Boolean messageWaiting = true;
//        while (messageWaiting) {
//            messageWaiting =
//                cdrEventsJmsTemplate.browse("direct-vm:cdrEvents.start",
//                                   (session, browser) -> browser.getEnumeration().hasMoreElements());
//        }
        // verify(solrUpdateProcessor).process(any(Exchange.class));
    }

    @Test
    public void testReorderAction() throws Exception {

    }

    @Test
    public void testIndexAction() throws Exception {

    }

    @Test
    public void testIndexPrimaryObjectAction() throws Exception {

    }

    @Test
    public void testReindexAction() throws Exception {

    }

    @Test
    public void testReindexInplaceAction() throws Exception {

    }

    @Test
    public void testPublishAction() throws Exception {

    }

    @Test
    public void testEditTypeAction() throws Exception {

    }

    private List<PID> pidList(int count) {
        List<PID> pidList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pidList.add(PIDs.get(UUID.randomUUID().toString()));
        }
        return pidList;
    }
}
