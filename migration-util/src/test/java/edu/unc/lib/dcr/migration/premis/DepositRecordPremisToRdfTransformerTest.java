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
package edu.unc.lib.dcr.migration.premis;

import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INGESTION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INGEST_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.METS_NORMAL_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.NORMALIZATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATE_MODS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE_UTC;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.deserializeLogFile;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.listEventResources;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.clamav;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.depositService;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.rdf.Premis;

/**
 * @author bbpennel
 *
 */
public class DepositRecordPremisToRdfTransformerTest extends AbstractPremisToRdfTransformerTest {

    protected DepositRecordPremisToRdfTransformer transformer;
    private PID depositServicePid;

    @Before
    public void setup() throws Exception {
        transformer = new DepositRecordPremisToRdfTransformer(objPid, premisLogger, premisDoc);
        depositServicePid = AgentPids.forSoftware(depositService);
    }

    @Test
    public void validVirusEvent() throws Exception {
        String detail = "28 files scanned for viruses.";
        Element eventEl = addEvent(premisDoc, VIRUS_CHECK_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, VIRUS_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.VirusCheck, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(AgentPids.forSoftware(clamav), eventResc);
    }

    @Test
    public void validIngestAsPidEvent() throws Exception {
        String detail = "ingested as PID:uuid:" + objPid.getId();
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, INGEST_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositServicePid, eventResc);
    }

    @Test
    public void invalidIngestAsPidEvent() throws Exception {
        String detail = "So many things got ingested";
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, INGEST_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void discardModsValidationEvent() throws Exception {
        String detail = "10 MODS records validated";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, VALIDATE_MODS_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void validMetsValidationEvent() throws Exception {
        String detail = "METS schema(s) validated";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, METS_NORMAL_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Validation, eventResc);
        assertEventDetail("METS schema validated", eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositServicePid, eventResc);
    }

    @Test
    public void unknownValidationEvent() throws Exception {
        String detail = "Validating the data";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, "validator");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void invalidNormalizationEvent() throws Exception {
        String detail = "Validating the data";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, "validator");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void validPidsAssignedToObjectsEvent() throws Exception {
        String detail = "Assigned PIDs to 3 objects";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, METS_NORMAL_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.InformationPackageCreation, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositServicePid, eventResc);
    }

    @Test
    public void validFormatNormalizationEvent() throws Exception {
        String detail = "Normalized deposit package from http://cdr.unc.edu/METS/profiles/Simple"
                + " to http://cdr.unc.edu/BAGIT/profiles/N3";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, METS_NORMAL_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail("ingested as format: http://cdr.unc.edu/METS/profiles/Simple", eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositServicePid, eventResc);
    }

    @Test
    public void unknownNormalizationEvent() throws Exception {
        String detail = "Normalizing the things";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, "normal_agent");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void noEvents() throws Exception {
        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void unknownEventType() throws Exception {
        String detail = "Preserving this thing";
        addEvent(premisDoc, "Preserveit", detail, EVENT_DATE);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void multipleEvents() throws Exception {
        String detail1 = "Normalized deposit package from http://cdr.unc.edu/METS/profiles/Simple"
                + " to http://cdr.unc.edu/BAGIT/profiles/N3";
        Element eventEl1 = addEvent(premisDoc, NORMALIZATION_TYPE, detail1, EVENT_DATE);
        addInitiatorAgent(eventEl1, METS_NORMAL_AGENT);
        String detail2 = "Assigned PIDs to 24 objects";
        String date2 = "2015-10-19T22:11:22.000Z";
        Element eventEl2 = addEvent(premisDoc, NORMALIZATION_TYPE, detail2, date2);
        addInitiatorAgent(eventEl2, METS_NORMAL_AGENT);
        String detail3 = "ingested as PID:uuid:" + objPid.getId();
        String date3 = "2015-10-19T22:41:01.000Z";
        Element eventEl3 = addEvent(premisDoc, INGESTION_TYPE, detail3, date3);
        addInitiatorAgent(eventEl3, INGEST_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(3, eventRescs.size());

        Resource eventResc1 = getResourceByEventDate(eventRescs, EVENT_DATE_UTC);
        assertEventType(Premis.Ingestion, eventResc1);
        assertEventDetail("ingested as format: http://cdr.unc.edu/METS/profiles/Simple", eventResc1);

        Resource eventResc2 = getResourceByEventDate(eventRescs, date2);
        assertEventType(Premis.InformationPackageCreation, eventResc2);
        assertEventDetail(detail2, eventResc2);

        Resource eventResc3 = getResourceByEventDate(eventRescs, date3);
        assertEventType(Premis.Ingestion, eventResc3);
        assertEventDetail(detail3, eventResc3);
    }

    @Test
    public void multipleEventTypes() throws Exception {
        String detail = "ingested as PID:uuid:" + objPid.getId();
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addInitiatorAgent(eventEl, "uuid:c52dc745-4b8b-4d87-af32-21348915c377");
        premisDoc.getRootElement().getChild("event", PREMIS_V2_NS)
            .addContent(new Element("eventType", PREMIS_V2_NS).setText(NORMALIZATION_TYPE));

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositServicePid, eventResc);
    }

    private void assertAgent(PID agentPid, Resource eventResc) {
        Resource agentResc = eventResc.getPropertyResourceValue(Premis.hasEventRelatedAgentExecutor);
        assertEquals(agentPid.getRepositoryPath(), agentResc.getURI());
    }

    private void addInitiatorAgent(Element eventEl, String agent) {
        addAgent(eventEl, "PID", INITIATOR_ROLE, agent);
    }
}
