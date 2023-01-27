package edu.unc.lib.boxc.operations.impl.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.rdf.Prov;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractFedoraObjectTest;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.impl.events.FilePremisLogger;

/**
 *
 * @author lfarrell
 *
 */
public class FilePremisLoggerTest extends AbstractFedoraObjectTest {
    private String depositUUID;
    private PID pid;
    private Resource eventType;
    private File premisFile;
    private PremisLogger premis;
    private Date date;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setup() throws Exception {

        depositUUID = UUID.randomUUID().toString();
        pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);
        eventType = Premis.VirusCheck;

        premisFile = new File(tmpFolder.toFile(), depositUUID + ".nt");
        premis = new FilePremisLogger(pid, premisFile, pidMinter);
        date = new Date();
    }

    @Test
    public void testEventbuilderCreation() {
        PremisEventBuilder builder = premis.buildEvent(null, eventType, date);

        assertTrue(premis instanceof PremisLogger, "Returned object is not a PremisLogger");
        assertTrue(builder instanceof PremisEventBuilder, "Returned object is not a PremisEventBuilder");
    }

    @Test
    public void testTripleWrite() throws IOException {
        String message = "Test event successfully added";

        PID softwarePid = AgentPids.forSoftware(SoftwareAgent.clamav);
        PID authPid = AgentPids.forPerson("agent2");
        Resource premisBuilder = premis.buildEvent(null, eventType, date)
                .addEventDetail(message)
                .addOutcome(true)
                .addSoftwareAgent(softwarePid)
                .addAuthorizingAgent(authPid)
                .create();

        premis.writeEvents(premisBuilder);

        InputStream in = new FileInputStream(this.premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "N-TRIPLES");
        Resource resource = model.getResource(premisBuilder.getURI());

        assertTrue(premisFile.exists(), "File doesn't exist");
        assertEquals(eventType, resource.getProperty(RDF.type).getObject(),
                "Virus check property event not written to file");
        assertEquals(message, resource.getProperty(Premis.note).getObject().toString(),
                "Virus check property message not written to file");
        assertEquals(Premis.Success, resource.getProperty(Premis.outcome).getResource(),
                "Virus check did not have success outcome");
        assertEquals(softwarePid.getRepositoryPath(),
                resource.getProperty(Premis.hasEventRelatedAgentExecutor).getResource().getURI(),
                "Virus check property depositing agent not written to file");
        assertEquals(authPid.getRepositoryPath(),
                resource.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource().getURI(),
                "Virus check property authorizing agent not written to file");

        Resource objResc = model.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(resource.hasProperty(Prov.used, objResc));
    }

    @Test
    public void testMultipleEvents() throws Exception {
        PID agentPid1 = AgentPids.forPerson("agent1");
        Resource event1 = premis.buildEvent(null, Premis.Normalization, date)
                .addEventDetail("Event 1")
                .addAuthorizingAgent(agentPid1)
                .write();

        PID agentPid2 = AgentPids.forSoftware(SoftwareAgent.clamav);
        Resource event2 = premis.buildEvent(null, Premis.VirusCheck, date)
                .addEventDetail("Event 2")
                .addSoftwareAgent(agentPid2)
                .write();

        InputStream in = new FileInputStream(premisFile);
        Model model = ModelFactory.createDefaultModel().read(in, null, "N-TRIPLES");

        Resource resc1 = model.getResource(event1.getURI());
        Resource resc2 = model.getResource(event2.getURI());

        assertNotEquals(resc1, resc2, "Events must have separate uris");

        assertEquals(Premis.Normalization, resc1.getProperty(RDF.type).getObject(),
                "Normalization type not written to file");
        assertEquals("Event 1", resc1.getProperty(Premis.note).getObject().toString(),
                "Event detail not written to file");
        assertEquals(agentPid1.getRepositoryPath(),
                resc1.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource().getURI(),
                "Authorizing agent not written to file");

        assertEquals(Premis.VirusCheck, resc2.getProperty(RDF.type).getObject(),
                "VirusCheck type not written to file");
        assertEquals("Event 2", resc2.getProperty(Premis.note).getObject().toString(),
                "Event detail not written to file");
        assertEquals(agentPid2.getRepositoryPath(),
                resc2.getProperty(Premis.hasEventRelatedAgentExecutor).getResource().getURI(),
                "Related agent not written to file");

        Resource objResc = model.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(resc1.hasProperty(Prov.used, objResc));
        assertTrue(resc2.hasProperty(Prov.used, objResc));
    }

    @Test
    public void testGetEventsModel() {

        PID agentPid1 = AgentPids.forSoftware(SoftwareAgent.depositService);
        Resource event1 = premis.buildEvent(null, Premis.Normalization, date)
                .addEventDetail("Event 1").addSoftwareAgent(agentPid1)
                .write();

        PID agentPid2 = AgentPids.forPerson("agent2");
        Resource event2 = premis.buildEvent(null, Premis.VirusCheck, date)
                .addEventDetail("Event 2").addAuthorizingAgent(agentPid2)
                .write();

        Model logModel = premis.getEventsModel();
        Resource logEvent1Resc = logModel.getResource(event1.getURI());
        Resource logEvent2Resc = logModel.getResource(event2.getURI());

        assertEquals(Premis.Normalization, logEvent1Resc.getProperty(RDF.type).getObject(),
                "Normalization type not written to file");
        assertEquals("Event 1", logEvent1Resc.getProperty(Premis.note).getString(),
                "Event detail not written to file");
        Resource event1AgentExecutor = logEvent1Resc.getProperty(Premis.hasEventRelatedAgentExecutor).getResource();
        assertEquals(agentPid1.getRepositoryPath(), event1AgentExecutor.getURI());

        assertEquals(Premis.VirusCheck, logEvent2Resc.getProperty(RDF.type).getObject(),
                "VirusCheck type not written to file");
        assertEquals("Event 2", logEvent2Resc.getProperty(Premis.note).getString(),
                "Event detail not written to file");
        Resource event2AgentAuth = logEvent2Resc.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource();
        assertEquals(agentPid2.getRepositoryPath(), event2AgentAuth.getURI());

        Resource objResc = logModel.getResource(pid.getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(logEvent1Resc.hasProperty(Prov.used, objResc));
        assertTrue(logEvent2Resc.hasProperty(Prov.used, objResc));
    }
}
