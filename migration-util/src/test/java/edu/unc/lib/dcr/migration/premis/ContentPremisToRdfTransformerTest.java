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

import static edu.unc.lib.dcr.migration.premis.Premis2Constants.CREATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.DELETION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.FIXITY_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.FIXITY_CHECK_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INGESTION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INGEST_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.METS_NORMAL_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.MIGRATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.NORMALIZATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.REPLICATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.SOFTWARE_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATE_MODS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE_UTC;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEventOutcome;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addLinkingObject;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.deserializeLogFile;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.listEventResources;
import static edu.unc.lib.dl.rdf.Premis.hasEventRelatedAgentAuthorizor;
import static edu.unc.lib.dl.rdf.Premis.hasEventRelatedAgentExecutor;
import static edu.unc.lib.dl.rdf.Premis.hasEventRelatedAgentImplementor;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.clamav;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.depositService;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.servicesAPI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * @author bbpennel
 */
public class ContentPremisToRdfTransformerTest extends AbstractPremisToRdfTransformerTest {

    protected ContentPremisToRdfTransformer transformer;

    @Before
    public void setup() throws Exception {
        transformer = new ContentPremisToRdfTransformer(objPid, premisLogger, premisDoc);
    }

    @Test
    public void ignoreModsValidation() throws Exception {
        String detail = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, VALIDATE_MODS_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void ignoreMetsValidation() throws Exception {
        String detail = "METS manifest validated against profile: http://cdr.unc.edu/METS/profiles/Simple";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", INITIATOR_ROLE, "Repository");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void invalidValidationEvent() throws Exception {
        String detail = "Object is not valid according to boxy";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, VALIDATE_MODS_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void virusScanValidation() throws Exception {
        String detail = "virus scan";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", INITIATOR_ROLE, "uuid:ff4fac12-0300-474b-841f-4032d041e333");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.VirusCheck, eventResc);
        assertEventDetail("File passed pre-ingest scan for viruses.", eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(clamav, eventResc);
        assertNoEventOutcome(eventResc);
    }

    @Test
    public void virusScanEvent() throws Exception {
        String detail = "File passed pre-ingest scan for viruses.";
        Element eventEl = addEvent(premisDoc, VIRUS_CHECK_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, VIRUS_AGENT);
        addAgent(eventEl, "Name", "ClamAV (ClamAV 0.100.2/25220/Tue Dec 18 21:49:44 2018)", SOFTWARE_ROLE);
        addEventOutcome(eventEl, "success", null);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.VirusCheck, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(clamav, eventResc);
        assertEventOutcomeSuccess(eventResc);
    }

    @Test
    public void ignoreModsToDcNormalization() throws Exception {
        String detail = "Metadata Object Description Schema (MODS) data transformed into Dublin Core (DC).";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void ignoreWorkbenchPidNormalization() throws Exception {
        String detail = "assigned persistently unique Fedora PID with UUID algorithm:"
                + " uuid:0c4a01a5-f8b6-4b4c-93e7-846c61d8e327";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void invalidNormalizationEvent() throws Exception {
        String detail = "Normalizing this object";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "nrmlz");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void metsPidAssignmentNormalization() throws Exception {
        String detail = "Assigned PID to object defined in a METS div";
        Element eventEl = addEvent(premisDoc, NORMALIZATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, METS_NORMAL_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.InformationPackageCreation, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService, eventResc);
    }

    @Test
    public void validPidIngestionEvent() throws Exception {
        String detail = "ingested as PID:uuid:08a5459c-3bbd-4461-9913-2d67a2a5a88";
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, INGEST_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService, eventResc);
    }

    @Test
    public void validChildAddedIngestionEvent() throws Exception {
        String detail = "added 2 child object(s) to this container";
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Ingestion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(depositService, eventResc);
    }

    @Test
    public void invalidIngestionEvent() throws Exception {
        String detail = "Ingesting all the things";
        Element eventEl = addEvent(premisDoc, INGESTION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, INGEST_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void moveMigrationEvent() throws Exception {
        String detail = "object moved from Container uuid:493d6c1c-b645-43a8-b301-5c14e2717313"
                + " to uuid:c59291a6-ad7a-4ad4-b89d-e2fe8acac744";
        Element eventEl = addEvent(premisDoc, MIGRATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.MetadataModification, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(servicesAPI, eventResc);
    }

    @Test
    public void changeTypeMigrationEvent() throws Exception {
        String detail = "Changed resource type from Folder to Collection";
        Element eventEl = addEvent(premisDoc, MIGRATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "username");
        PID agentPid = AgentPids.forPerson("username");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.MetadataModification, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(agentPid, hasEventRelatedAgentImplementor, eventResc);
    }

    @Test
    public void renameMigrationEvent() throws Exception {
        String detail = "Object renamed to My Favorite Folder";
        Element eventEl = addEvent(premisDoc, MIGRATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", "Initiator", "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.FilenameChange, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(servicesAPI, eventResc);
    }

    @Test
    public void invalidMigration() throws Exception {
        String detail = "Migrating to another place";
        Element eventEl = addEvent(premisDoc, MIGRATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "username");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void destroyEvent() throws Exception {
        String detail = "Deleted 0contained object(s).";
        Element eventEl = addEvent(premisDoc, DELETION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Deletion, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(servicesAPI, eventResc);
    }

    @Test
    public void invalidDeletionEvent() throws Exception {
        String detail = "Deleting this thing";
        Element eventEl = addEvent(premisDoc, DELETION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void containerCreationEvent() throws Exception {
        String detail = "Container created";
        Element eventEl = addEvent(premisDoc, CREATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "username");
        PID agentPid = AgentPids.forPerson("username");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.Creation, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(agentPid, hasEventRelatedAgentImplementor, eventResc);
    }

    @Test
    public void sipCreationEvent() throws Exception {
        String detail = "SIP created";
        Element eventEl = addEvent(premisDoc, CREATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", "Creator", "CDR Workbench");
        addAgent(eventEl, "Name", "Creator", "username");
        PID agentPid = AgentPids.forPerson("username");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.InformationPackageCreation, eventResc);
        assertEventDetail(detail, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent(SoftwareAgent.curatorsWorkbench, eventResc);
        assertAgent(agentPid, hasEventRelatedAgentAuthorizor, eventResc);
    }

    @Test
    public void unknownCreationEvent() throws Exception {
        String detail = "Created the objects";
        Element eventEl = addEvent(premisDoc, CREATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "PID", INITIATOR_ROLE, "creator");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void objectMissingReplicationEvent() throws Exception {
        Element eventEl = addEvent(premisDoc, REPLICATION_TYPE, null, EVENT_DATE);
        String eventNote = "The object was not found on the resource theResource.";
        addEventOutcome(eventEl, "MISSING", eventNote);
        addAgent(eventEl, "Class", "Software", FIXITY_AGENT);
        addAgent(eventEl, "Jargon version", "Software", "2.2");
        addAgent(eventEl, "iRODS release version", "Software", "rods3.2");
        String linkingObj1 = "/path/to/missing/myobj+MD_EVENTS+MD_EVENTS.0";
        addLinkingObject(eventEl, "iRODS object path", linkingObj1);
        String linkingObj2 = "myobj/MD_EVENTS";
        addLinkingObject(eventEl, "PID", linkingObj2);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.FixityCheck, eventResc);
        assertEventDetail(eventNote, eventResc);
        assertEventDetail("iRODS object path " + linkingObj1, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent("Class " + FIXITY_AGENT, eventResc);
        assertAgent("Jargon version 2.2", eventResc);
        assertAgent("iRODS release version rods3.2", eventResc);
        assertNoEventOutcome(eventResc);
    }

    @Test
    public void invalidReplicationEvent() throws Exception {
        Element eventEl = addEvent(premisDoc, REPLICATION_TYPE, null, EVENT_DATE);
        String eventNote = "Replicating things to places";
        addEventOutcome(eventEl, "Replicated", eventNote);
        addAgent(eventEl, "Class", "Software", FIXITY_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void fixityCheckFailEvent() throws Exception {
        Element eventEl = addEvent(premisDoc, FIXITY_CHECK_TYPE, null, EVENT_DATE);
        String eventNote = "The checksum of the object on the resource itsResc didn't match"
                + " the expected value of 347eb92e5e6a1673dbd5a9c466dbfd5e."
                + " iRODS error: USER_CHKSUM_MISMATCH -314000";
        addEventOutcome(eventEl, "FAILED", eventNote);
        addAgent(eventEl, "Class", "Software", FIXITY_AGENT);
        addAgent(eventEl, "Jargon version", "Software", "2.2");
        addAgent(eventEl, "iRODS release version", "Software", "rods3.2");
        String linkingObj1 = "/path/to/missing/myobj+MD_EVENTS+MD_EVENTS.0";
        addLinkingObject(eventEl, "iRODS object path", linkingObj1);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEventType(Premis.FixityCheck, eventResc);
        assertEventDetail(eventNote, eventResc);
        assertEventDetail("iRODS object path " + linkingObj1, eventResc);
        assertEventDateTime(EVENT_DATE_UTC, eventResc);
        assertAgent("Class " + FIXITY_AGENT, eventResc);
        assertAgent("Jargon version 2.2", eventResc);
        assertAgent("iRODS release version rods3.2", eventResc);
        assertEventOutcomeFail(eventResc);
    }

    @Test
    public void invalidFixityCheckEvent() throws Exception {
        Element eventEl = addEvent(premisDoc, FIXITY_CHECK_TYPE, null, EVENT_DATE);
        String eventNote = "Fixity checking time";
        addEventOutcome(eventEl, "FAILED", eventNote);
        addAgent(eventEl, "Class", "Software", FIXITY_AGENT);

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void multipleEvents() throws Exception {
        String detail0 = "File passed pre-ingest scan for viruses.";
        Element eventEl0 = addEvent(premisDoc, VIRUS_CHECK_TYPE, detail0, EVENT_DATE);
        addAgent(eventEl0, "PID", INITIATOR_ROLE, VIRUS_AGENT);
        addAgent(eventEl0, "Name", "ClamAV (ClamAV 0.100.2/25220/Tue Dec 18 21:49:44 2018)", SOFTWARE_ROLE);

        // This one is ignored
        String date1 = "2015-10-19T22:11:22.000Z";
        String detail1 = "Metadata Object Description Schema (MODS) data transformed into Dublin Core (DC).";
        Element eventEl1 = addEvent(premisDoc, NORMALIZATION_TYPE, detail1, date1);
        addAgent(eventEl1, "PID", INITIATOR_ROLE, "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        String detail2 = "ingested as PID:uuid:08a5459c-3bbd-4461-9913-2d67a2a5a88";
        String date2 = "2015-10-19T22:11:27.000Z";
        Element eventEl2 = addEvent(premisDoc, INGESTION_TYPE, detail2, date2);
        addAgent(eventEl2, "PID", INITIATOR_ROLE, INGEST_AGENT);

        String detail3 = "object moved from Container uuid:493d6c1c-b645-43a8-b301-5c14e2717313"
                + " to uuid:c59291a6-ad7a-4ad4-b89d-e2fe8acac744";
        String date3 = "2017-08-19T10:18:11.000Z";
        Element eventEl3 = addEvent(premisDoc, MIGRATION_TYPE, detail3, date3);
        addAgent(eventEl3, "PID", "Initiator", "uuid:4e6425d9-79d4-4d94-af51-c49ee7f31cf8");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(3, eventRescs.size());

        Resource eventResc = getResourceByEventDate(eventRescs, EVENT_DATE_UTC);
        assertEventType(Premis.VirusCheck, eventResc);
        assertEventDetail(detail0, eventResc);
        assertAgent(clamav, eventResc);

        Resource eventResc2 = getResourceByEventDate(eventRescs, date2);
        assertEventType(Premis.Ingestion, eventResc2);
        assertEventDetail(detail2, eventResc2);
        assertAgent(depositService, eventResc2);

        Resource eventResc3 = getResourceByEventDate(eventRescs, date3);
        assertEventType(Premis.MetadataModification, eventResc3);
        assertEventDetail(detail3, eventResc3);
        assertAgent(servicesAPI, eventResc3);
    }

    @Test
    public void noEvents() throws Exception {
        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void unknownEvent() throws Exception {
        Element eventEl = addEvent(premisDoc, "other_event", "details", EVENT_DATE);
        addAgent(eventEl, "Class", "Software", "dcr");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    @Test
    public void noEventType() throws Exception {
        Element eventEl = addEvent(premisDoc, null, "Doing something", EVENT_DATE);
        addAgent(eventEl, "Class", "Software", "secret_agent");

        transformer.compute();

        Model model = deserializeLogFile(logFile);
        List<Resource> eventRescs = listEventResources(objPid, model);
        assertEquals(0, eventRescs.size());
    }

    private void assertAgent(SoftwareAgent agent, Resource eventResc) {
        assertAgent(AgentPids.forSoftware(agent), hasEventRelatedAgentExecutor, eventResc);
    }

    private void assertAgent(String agentName, Resource eventResc) {
        assertAgent(AgentPids.forSoftware(agentName), hasEventRelatedAgentExecutor, eventResc);
    }

    private void assertAgent(PID agentPid, Property hasProperty, Resource eventResc) {
        String expectedAgentUri = agentPid.getRepositoryPath();
        for (Statement stmt: eventResc.listProperties(hasProperty).toList()) {
            Resource agentResc = stmt.getResource();
            if (agentResc.getURI().equals(expectedAgentUri)) {
                return;
            }
        }

        fail(String.format("No %s relation to agent with value %s present",
                hasProperty, agentPid.getQualifiedId()));
    }
}
