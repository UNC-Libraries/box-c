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
package edu.unc.lib.dcr.migration.deposit;

import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.PREMIS_DS;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.createPremisDoc;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.getEventByType;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.listEventResources;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newOutputStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.Rdf;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * @author bbpennel
 */
public abstract class AbstractDepositRecordTransformationIT {
    static {
        System.setProperty("fcrepo.properties.management", "relaxed");
    }

    protected final static Date DEFAULT_CREATED_DATE = DateTimeUtil.parseUTCToDate(
            FoxmlDocumentBuilder.DEFAULT_CREATED_DATE);
    protected final static Date DEFAULT_MODIFIED_DATE = DateTimeUtil.parseUTCToDate(
            FoxmlDocumentBuilder.DEFAULT_LAST_MODIFIED);

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    protected Path objectsPath;

    protected Path datastreamsPath;

    protected Path serializeFoxml(PID pid, Document doc) throws IOException {
        Path xmlPath = objectsPath.resolve("uuid_" + pid.getId());
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(doc, outStream);
        return xmlPath;
    }

    protected Path writeManifestFile(PID pid, String dsName, String content) throws IOException {
        Path dsPath = datastreamsPath.resolve("uuid_" + pid.getId() + "+" + dsName + "+" + dsName + ".0");
        FileUtils.writeStringToFile(dsPath.toFile(), content, UTF_8);
        return dsPath;
    }

    protected Model createModelWithTypes(PID pid, ContentModel... models) {
        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(pid));
        for (ContentModel contentModel : models) {
            resc.addProperty(hasModel.getProperty(), contentModel.getResource());
        }
        return model;
    }

    protected void addPremisLog(PID originalPid) throws IOException {
        Document premisDoc = createPremisDoc(originalPid);
        String detail = "virus scan";
        Element eventEl = addEvent(premisDoc, VIRUS_CHECK_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", INITIATOR_ROLE, "virusscanner");

        String premisDsName = "uuid_" + originalPid.getId() + "+" + PREMIS_DS + "+" + PREMIS_DS + ".0";
        Path xmlPath = datastreamsPath.resolve(premisDsName);
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(premisDoc, outStream);
    }

    protected BinaryObject getManifestByName(List<BinaryObject> binList, String dsName) {
        return binList.stream()
                .filter(b -> b.getPid().getComponentPath().endsWith(dsName.toLowerCase()))
                .findFirst()
                .get();
    }

    protected void assertManifestDetails(Date expectedTimestamp, String expectedMimetype,
            String expectedContent, BinaryObject manifestBin) throws IOException {
        assertEquals(expectedTimestamp, manifestBin.getLastModified());
        assertEquals(expectedTimestamp, manifestBin.getCreatedDate());
        assertEquals(expectedMimetype, manifestBin.getMimetype());
        assertEquals(expectedContent, IOUtils.toString(manifestBin.getBinaryStream(), UTF_8));
    }

    protected void assertPremisTransformed(DepositRecord depRec) throws IOException {
        PremisLogger premisLog = depRec.getPremisLog();
        Model eventsModel = premisLog.getEventsModel();
        List<Resource> eventRescs = listEventResources(depRec.getPid(), eventsModel);
        assertEquals(2, eventRescs.size());

        Resource virusEventResc = getEventByType(eventRescs, Premis.VirusCheck);
        assertNotNull("Virus event was not present in premis log",
                virusEventResc);
        Resource migrationEventResc = getEventByType(eventRescs, Premis.Ingestion);
        assertTrue("Missing migration event note",
                migrationEventResc.hasProperty(Premis.note, "Object migrated from Boxc 3 to Boxc 5"));
        Resource agentResc = migrationEventResc.getProperty(Premis.hasEventRelatedAgentExecutor).getResource();
        assertNotNull("Migration agent not set", agentResc);
        assertEquals(SoftwareAgent.migrationUtil.getFullname(), agentResc.getProperty(Rdf.label).getString());
    }
}
