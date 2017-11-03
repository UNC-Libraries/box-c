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
package edu.unc.lib.dl.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraTest;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.PremisAgentType;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 *
 * @author lfarrell
 *
 */
public class FilePremisLoggerTest extends AbstractFedoraTest {
    private String depositUUID;
    private PID pid;
    private Resource eventType;
    private File premisFile;
    private PremisLogger premis;
    private Date date;
    @Mock
    private RepositoryObjectLoader repoObjLoader;

    @Before
    public void setup() throws Exception {

        depositUUID = UUID.randomUUID().toString();
        pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);
        eventType = Premis.VirusCheck;
        premisFile = File.createTempFile(depositUUID, ".ttl");
        premisFile.deleteOnExit();
        premis = new FilePremisLogger(pid, premisFile, pidMinter, repoObjLoader, repoObjFactory, driver);
        date = new Date();
    }

    @Test
    public void testEventbuilderCreation() {
        PremisEventBuilder builder = premis.buildEvent(eventType, date);

        assertTrue("Returned object is not a PremisLogger", premis instanceof PremisLogger);
        assertTrue("Returned object is not a PremisEventBuilder", builder instanceof PremisEventBuilder);
    }

    @Test
    public void testTripleWrite() throws IOException {
        String message = "Test event successfully added";
        String detailedNote = "No viruses found";

        Resource premisBuilder = premis.buildEvent(eventType, date)
                .addEventDetail(message)
                .addEventDetailOutcomeNote(detailedNote)
                .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                .addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
                .create();

        premis.writeEvent(premisBuilder);

        InputStream in = new FileInputStream(this.premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "TURTLE");
        Resource resource = model.getResource(premisBuilder.getURI());

        assertTrue("File doesn't exist", premisFile.exists());
        assertEquals("Virus check property event not written to file", eventType,
                resource.getProperty(Premis.hasEventType).getObject());
        assertEquals("Virus check property message not written to file", message,
                resource.getProperty(Premis.hasEventDetail).getObject().toString());
        assertEquals("Virus check property detailed note not written to file", detailedNote,
                resource.getProperty(Premis.hasEventOutcomeDetailNote).getObject().toString());
        assertEquals("Virus check property depositing agent not written to file", SoftwareAgent.clamav.getFullname(),
                resource.getProperty(Premis.hasEventRelatedAgentExecutor)
                .getProperty(Premis.hasAgentName).getObject().toString());
        assertEquals("Virus check property authorizing agent not written to file", SoftwareAgent.depositService.getFullname(),
                resource.getProperty(Premis.hasEventRelatedAgentAuthorizor)
                .getProperty(Premis.hasAgentName).getObject().toString());
    }

    @Test
    public void testMultipleEvents() throws Exception {
        Resource event1 = premis.buildEvent(Premis.Normalization, date)
                .addEventDetail("Event 1")
                .addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
                .write();

        Resource event2 = premis.buildEvent(Premis.VirusCheck, date)
                .addEventDetail("Event 2")
                .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                .write();

        InputStream in = new FileInputStream(premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "TURTLE");

        Resource resc1 = model.getResource(event1.getURI());
        Resource resc2 = model.getResource(event2.getURI());

        assertNotEquals("Events must have separate uris", resc1, resc2);

        assertEquals("Normalization type not written to file", Premis.Normalization,
                resc1.getProperty(Premis.hasEventType).getObject());
        assertEquals("Event detail not written to file", "Event 1",
                resc1.getProperty(Premis.hasEventDetail).getObject().toString());
        assertEquals("Authorizing agent not written to file", SoftwareAgent.depositService.getFullname(),
                resc1.getProperty(Premis.hasEventRelatedAgentAuthorizor)
                        .getProperty(Premis.hasAgentName).getObject().toString());

        assertEquals("VirusCheck type not written to file", Premis.VirusCheck,
                resc2.getProperty(Premis.hasEventType).getObject());
        assertEquals("Event detail not written to file", "Event 2",
                resc2.getProperty(Premis.hasEventDetail).getObject().toString());
        assertEquals("Related agent not written to file", SoftwareAgent.clamav.getFullname(),
                resc2.getProperty(Premis.hasEventRelatedAgentExecutor)
                        .getProperty(Premis.hasAgentName).getObject().toString());
    }

    @Test
    public void getEventsTest() {

        Resource event1 = premis.buildEvent(Premis.Normalization, date)
                .addEventDetail("Event 1").addSoftwareAgent("Agent 1")
                .write();

        Resource event2 = premis.buildEvent(Premis.VirusCheck, date)
                .addEventDetail("Event 2").addAuthorizingAgent("Agent 2")
                .write();

        List<PremisEventObject> events = premis.getEvents();
        assertEquals(2, events.size());

        PremisEventObject eventObj1 = findEventByPid(events, PIDs.get(event1.getURI()));
        PremisEventObject eventObj2 = findEventByPid(events, PIDs.get(event2.getURI()));

        assertEquals("Normalization type not written to file", Premis.Normalization,
                eventObj1.getResource().getProperty(Premis.hasEventType).getObject());
        assertEquals("Event detail not written to file", "Event 1",
                eventObj1.getResource().getProperty(Premis.hasEventDetail).getObject().toString());
        assertEquals("Software agent not written to file", PremisAgentType.Software,
                eventObj1.getResource().getProperty(Premis.hasEventRelatedAgentExecutor).getResource()
                .getProperty(Premis.hasAgentType).getObject());
        assertEquals("Software agent name not written to file", "Agent 1",
                eventObj1.getResource().getProperty(Premis.hasEventRelatedAgentExecutor).getResource()
                .getProperty(Premis.hasAgentName).getObject().toString());

        assertEquals("VirusCheck type not written to file", Premis.VirusCheck,
                eventObj2.getResource().getProperty(Premis.hasEventType).getObject());
        assertEquals("Event detail not written to file", "Event 2",
                eventObj2.getResource().getProperty(Premis.hasEventDetail).getObject().toString());
        assertEquals("Authorizing agent not written to file", PremisAgentType.Person,
                eventObj2.getResource().getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource()
                .getProperty(Premis.hasAgentType).getObject());
        assertEquals("Authorizing agent name not written to file", "Agent 2",
                eventObj2.getResource().getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource()
                .getProperty(Premis.hasAgentName).getObject().toString());
    }

    protected PremisEventObject findEventByPid(List<PremisEventObject> events, PID pid) {
        return events.stream()
                .filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
