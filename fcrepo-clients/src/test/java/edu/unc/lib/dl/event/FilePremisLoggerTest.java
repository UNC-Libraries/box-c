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
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraTest;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.PremisAgentType;
import edu.unc.lib.dl.rdf.Prov;
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

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {

        depositUUID = UUID.randomUUID().toString();
        pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);
        eventType = Premis.VirusCheck;

        tmpFolder.create();
        premisFile = new File(tmpFolder.getRoot(), depositUUID + ".nt");
        premis = new FilePremisLogger(pid, premisFile, pidMinter);
        date = new Date();
    }

    @Test
    public void testEventbuilderCreation() {
        PremisEventBuilder builder = premis.buildEvent(null, eventType, date);

        assertTrue("Returned object is not a PremisLogger", premis instanceof PremisLogger);
        assertTrue("Returned object is not a PremisEventBuilder", builder instanceof PremisEventBuilder);
    }

    @Test
    public void testTripleWrite() throws IOException {
        String message = "Test event successfully added";

        Resource premisBuilder = premis.buildEvent(null, eventType, date)
                .addEventDetail(message)
                .addOutcome(true)
                .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                .addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
                .create();

        premis.writeEvents(premisBuilder);

        InputStream in = new FileInputStream(this.premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "N-TRIPLES");
        Resource resource = model.getResource(premisBuilder.getURI());

        assertTrue("File doesn't exist", premisFile.exists());
        assertEquals("Virus check property event not written to file", eventType,
                resource.getProperty(RDF.type).getObject());
        assertEquals("Virus check property message not written to file", message,
                resource.getProperty(Premis.note).getObject().toString());
        assertEquals("Virus check did not have success outcome", Premis.Success,
                resource.getProperty(Premis.outcome).getResource());
        assertEquals("Virus check property depositing agent not written to file", SoftwareAgent.clamav.getFullname(),
                resource.getProperty(Premis.hasEventRelatedAgentExecutor)
                .getProperty(Premis.hasAgentName).getObject().toString());
        assertEquals("Virus check property authorizing agent not written to file", SoftwareAgent.depositService.getFullname(),
                resource.getProperty(Premis.hasEventRelatedAgentAuthorizor)
                .getProperty(Premis.hasAgentName).getObject().toString());

        Resource objResc = model.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(objResc.hasProperty(Prov.wasUsedBy, resource));
    }

    @Test
    public void testMultipleEvents() throws Exception {
        Resource event1 = premis.buildEvent(null, Premis.Normalization, date)
                .addEventDetail("Event 1")
                .addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
                .write();

        Resource event2 = premis.buildEvent(null, Premis.VirusCheck, date)
                .addEventDetail("Event 2")
                .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                .write();

        InputStream in = new FileInputStream(premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "N-TRIPLES");

        Resource resc1 = model.getResource(event1.getURI());
        Resource resc2 = model.getResource(event2.getURI());

        assertNotEquals("Events must have separate uris", resc1, resc2);

        assertEquals("Normalization type not written to file", Premis.Normalization,
                resc1.getProperty(RDF.type).getObject());
        assertEquals("Event detail not written to file", "Event 1",
                resc1.getProperty(Premis.note).getObject().toString());
        assertEquals("Authorizing agent not written to file", SoftwareAgent.depositService.getFullname(),
                resc1.getProperty(Premis.hasEventRelatedAgentAuthorizor)
                        .getProperty(Premis.hasAgentName).getObject().toString());

        assertEquals("VirusCheck type not written to file", Premis.VirusCheck,
                resc2.getProperty(RDF.type).getObject());
        assertEquals("Event detail not written to file", "Event 2",
                resc2.getProperty(Premis.note).getObject().toString());
        assertEquals("Related agent not written to file", SoftwareAgent.clamav.getFullname(),
                resc2.getProperty(Premis.hasEventRelatedAgentExecutor)
                        .getProperty(Premis.hasAgentName).getObject().toString());

        Resource objResc = model.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(objResc.hasProperty(Prov.wasUsedBy, resc1));
        assertTrue(objResc.hasProperty(Prov.wasUsedBy, resc2));
    }

    @Test
    public void testGetEventsModel() {

        Resource event1 = premis.buildEvent(null, Premis.Normalization, date)
                .addEventDetail("Event 1").addSoftwareAgent("Agent 1")
                .write();

        Resource event2 = premis.buildEvent(null, Premis.VirusCheck, date)
                .addEventDetail("Event 2").addAuthorizingAgent("Agent 2")
                .write();

        Model logModel = premis.getEventsModel();
        Resource logEvent1Resc = logModel.getResource(event1.getURI());
        Resource logEvent2Resc = logModel.getResource(event2.getURI());

        assertEquals("Normalization type not written to file", Premis.Normalization,
                logEvent1Resc.getProperty(RDF.type).getObject());
        assertEquals("Event detail not written to file", "Event 1",
                logEvent1Resc.getProperty(Premis.note).getString());
        assertEquals("Software agent not written to file", PremisAgentType.Software,
                logEvent1Resc.getProperty(Premis.hasEventRelatedAgentExecutor).getResource()
                    .getProperty(Premis.hasAgentType).getObject());
        assertEquals("Software agent name not written to file", "Agent 1",
                logEvent1Resc.getProperty(Premis.hasEventRelatedAgentExecutor).getResource()
                    .getProperty(Premis.hasAgentName).getObject().toString());

        assertEquals("VirusCheck type not written to file", Premis.VirusCheck,
                logEvent2Resc.getProperty(RDF.type).getObject());
        assertEquals("Event detail not written to file", "Event 2",
                logEvent2Resc.getProperty(Premis.note).getString());
        assertEquals("Authorizing agent not written to file", PremisAgentType.Person,
                logEvent2Resc.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource()
                    .getProperty(Premis.hasAgentType).getObject());
        assertEquals("Authorizing agent name not written to file", "Agent 2",
                logEvent2Resc.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource()
                    .getProperty(Premis.hasAgentName).getObject().toString());

        Resource objResc = logModel.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(objResc.hasProperty(Prov.wasUsedBy, logEvent1Resc));
        assertTrue(objResc.hasProperty(Prov.wasUsedBy, logEvent2Resc));
    }
}
