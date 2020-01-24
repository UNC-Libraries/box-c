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
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.METS_NORMAL_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.NORMALIZATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATE_MODS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dl.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.dl.util.DateTimeUtil.parseUTCToDate;
import static edu.unc.lib.dl.util.RDFModelUtil.createModel;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.clamav;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.depositService;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XSI_NS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.PremisAgentType;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * @author bbpennel
 *
 */
public class DepositRecordPremisToRdfTransformerTest {

    private static final String EVENT_DATE = "2015-10-19T17:06:22";
    private static final String EVENT_DATE_UTC = formatDateToUTC(parseUTCToDate(EVENT_DATE));

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private File logFile;

    private PID objPid;

    private DepositRecordPremisToRdfTransformer transformer;

    private Document premisDoc;

    private PremisLogger premisLogger;
    private RepositoryPIDMinter pidMinter;

    @Before
    public void setup() throws Exception {
        pidMinter = new RepositoryPIDMinter();

        objPid = makePid();
        logFile = tmpFolder.newFile(objPid.getId() + ".nt");
        premisDoc = createPremisDoc(objPid);
        premisLogger = new FilePremisLogger(objPid, logFile, pidMinter);

        transformer = new DepositRecordPremisToRdfTransformer(objPid, premisLogger, premisDoc);
    }

    @Test
    public void validVirusEvent() throws Exception {
        String detail = "28 files scanned for viruses.";
        addEvent(premisDoc, VIRUS_CHECK_TYPE, detail, EVENT_DATE, VIRUS_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.VirusCheck, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(clamav.getFullname(), eventResc);
    }

    @Test
    public void validIngestAsPidEvent() throws Exception {
        String detail = "ingested as PID:uuid:" + objPid.getId();
        addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE, INGEST_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService.getFullname(), eventResc);
    }

    @Test
    public void invalidIngestAsPidEvent() throws Exception {
        String detail = "So many things got ingested";
        addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE, INGEST_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void discardModsValidationEvent() throws Exception {
        String detail = "10 MODS records validated";
        addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE, VALIDATE_MODS_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void validMetsValidationEvent() throws Exception {
        String detail = "METS schema(s) validated";
        addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE, METS_NORMAL_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Validation, eventResc);
        assertEventDetail("METS schema validated", eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService.getFullname(), eventResc);
    }

    @Test
    public void unknownValidationEvent() throws Exception {
        String detail = "Validating the data";
        addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE, "validator");

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void invalidNormalizationEvent() throws Exception {
        String detail = "Validating the data";
        addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE, "validator");

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void validPidsAssignedToObjectsEvent() throws Exception {
        String detail = "Assigned PIDs to 3 objects";
        addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE, METS_NORMAL_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.InformationPackageCreation, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService.getFullname(), eventResc);
    }

    @Test
    public void validFormatNormalizationEvent() throws Exception {
        String detail = "Normalized deposit package from http://cdr.unc.edu/METS/profiles/Simple"
                + " to http://cdr.unc.edu/BAGIT/profiles/N3";
        addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE, METS_NORMAL_AGENT);

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail("ingested as format: http://cdr.unc.edu/METS/profiles/Simple", eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService.getFullname(), eventResc);
    }

    @Test
    public void unknownNormalizationEvent() throws Exception {
        String detail = "Normalizing the things";
        addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE, "normal_agent");

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void noEvents() throws Exception {
        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void unknownEventType() throws Exception {
        String detail = "Preserving this thing";
        addEvent(premisDoc, "Preserveit", detail, EVENT_DATE, "secret_agent");

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void multipleEvents() throws Exception {
        String detail1 = "Normalized deposit package from http://cdr.unc.edu/METS/profiles/Simple"
                + " to http://cdr.unc.edu/BAGIT/profiles/N3";
        addEvent(premisDoc, NORMALIZATION_TYPE, detail1, EVENT_DATE, METS_NORMAL_AGENT);
        String detail2 = "Assigned PIDs to 24 objects";
        String date2 = "2015-10-19T22:11:22.000Z";
        addEvent(premisDoc, NORMALIZATION_TYPE, detail2, date2, METS_NORMAL_AGENT);
        String detail3 = "ingested as PID:uuid:" + objPid.getId();
        String date3 = "2015-10-19T22:41:01.000Z";
        addEvent(premisDoc, INGESTION_TYPE, detail3, date3, INGEST_AGENT);

        transformer.transform();

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
        addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE, "uuid:c52dc745-4b8b-4d87-af32-21348915c377");
        premisDoc.getRootElement().getChild("event", PREMIS_V2_NS)
            .addContent(new Element("eventType", PREMIS_V2_NS).setText(NORMALIZATION_TYPE));

        transformer.transform();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService.getFullname(), eventResc);
    }

    private Resource getResourceByEventDate(List<Resource> rescs, String eventDate) {
        return rescs.stream().filter(r -> eventDate.equals(r.getProperty(Premis.hasEventDateTime).getString()))
                .findFirst().get();
    }

    private void assertEventType(Resource expectedType, Resource eventResc) {
        assertEquals("Event type did not match expected value",
                expectedType, eventResc.getPropertyResourceValue(Premis.hasEventType));
    }

    private void assertEventDetail(String expected, Resource eventResc) {
        assertEquals("Event detail message did not match expected value",
                expected, eventResc.getProperty(Premis.hasEventDetail).getString());
    }

    private void assertEventDateTime(String expected, Resource eventResc) {
        assertEquals("Event date time did not match expected value",
                expected, eventResc.getProperty(Premis.hasEventDateTime).getString());
    }

    private void assertAgent(String agentName, Resource eventResc) {
        Resource agentResc = eventResc.getPropertyResourceValue(Premis.hasEventRelatedAgentExecutor);
        assertEquals(PremisAgentType.Software, agentResc.getPropertyResourceValue(Premis.hasAgentType));
        assertEquals(agentName, agentResc.getProperty(Premis.hasAgentName).getString());
    }

    private Model deserializeLogFile(File logFile) throws IOException {
        return createModel(new FileInputStream(logFile), "N-TRIPLE");
    }

    private List<Resource> listEventResources(PID pid, Model model) {
        Resource objResc = model.getResource(objPid.getRepositoryPath());

        return objResc.listProperties(Premis.hasEvent).toList().stream()
                .map(Statement::getResource)
                .collect(toList());
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private Document createPremisDoc(PID pid) {
        Document doc = new Document();
        doc.addContent(new Element("premis", PREMIS_V2_NS)
                .addContent(new Element("object", PREMIS_V2_NS)
                        .setAttribute("type", "representation", XSI_NS)
                        .addContent(new Element("objectIdentifier", PREMIS_V2_NS)
                                .addContent(new Element("objectIdentifierType", PREMIS_V2_NS).setText("PID"))
                                .addContent(new Element("objectIdentifierValue", PREMIS_V2_NS)
                                        .setText("uuid:" + pid.getId())))));
        return doc;
    }

    private void addEvent(Document doc, String type, String detail, String dateTime, String agent) {
        Element premisEl = doc.getRootElement();

        Element eventEl = new Element("event", PREMIS_V2_NS);
        premisEl.addContent(eventEl);

        String eventId = "urn:uuid:" + UUID.randomUUID().toString();
        eventEl.addContent(new Element("eventIdentifier", PREMIS_V2_NS)
                .addContent(new Element("eventIdentifierType", PREMIS_V2_NS).setText("URN"))
                .addContent(new Element("eventIdentifierValue", PREMIS_V2_NS).setText(eventId)));

        if (type != null) {
            eventEl.addContent(new Element("eventType", PREMIS_V2_NS).setText(type));
        }

        if (detail != null) {
            eventEl.addContent(new Element("eventDetail", PREMIS_V2_NS).setText(detail));
        }

        if (dateTime == null) {
            dateTime = DateTimeUtil.formatDateToUTC(new Date());
        }
        eventEl.addContent(new Element("eventDateTime", PREMIS_V2_NS).setText(dateTime));

        eventEl.addContent(new Element("linkingAgentIdentifier", PREMIS_V2_NS)
                .addContent(new Element("linkingAgentIdentifierType", PREMIS_V2_NS).setText("PID"))
                .addContent(new Element("linkingAgentRole", PREMIS_V2_NS).setText("Initiator"))
                .addContent(new Element("linkingAgentIdentifierValue", PREMIS_V2_NS).setText(agent)));
    }
}
