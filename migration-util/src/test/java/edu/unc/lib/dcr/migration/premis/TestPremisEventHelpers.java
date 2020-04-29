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

import static edu.unc.lib.dcr.migration.premis.PremisTransformationService.getTransformedPremisPath;
import static edu.unc.lib.dl.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.dl.util.DateTimeUtil.parseUTCToDate;
import static edu.unc.lib.dl.util.RDFModelUtil.createModel;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XSI_NS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jgroups.util.UUID;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Prov;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Test helper methods for constructing and processing premis XML documents
 *
 * @author bbpennel
 *
 */
public class TestPremisEventHelpers {

    public static final String EVENT_DATE = "2015-10-19T17:06:22";
    public static final String EVENT_DATE_UTC = formatDateToUTC(parseUTCToDate(EVENT_DATE));

    private TestPremisEventHelpers() {
    }

    public static Document createPremisDoc(PID pid) {
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

    public static Element addEvent(Document doc, String type, String detail, String dateTime) {
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

        return eventEl;
    }

    public static void addAgent(Element eventEl, String agentType, String agentRole, String agentVal) {
        eventEl.addContent(new Element("linkingAgentIdentifier", PREMIS_V2_NS)
                .addContent(new Element("linkingAgentIdentifierType", PREMIS_V2_NS).setText(agentType))
                .addContent(new Element("linkingAgentRole", PREMIS_V2_NS).setText(agentRole))
                .addContent(new Element("linkingAgentIdentifierValue", PREMIS_V2_NS).setText(agentVal)));
    }

    public static void addEventOutcome(Element eventEl, String outcome, String note) {
        Element outcomeInfoEl = new Element("eventOutcomeInformation", PREMIS_V2_NS);
        if (note != null) {
            outcomeInfoEl.addContent(new Element("eventOutcomeDetail", PREMIS_V2_NS)
                    .addContent(new Element("eventOutcomeDetailNote", PREMIS_V2_NS).setText(note)));
        }
        if (outcome != null) {
            outcomeInfoEl.addContent(new Element("eventOutcome", PREMIS_V2_NS).setText(outcome));
        }
        eventEl.addContent(outcomeInfoEl);
    }

    public static void addLinkingObject(Element eventEl, String idType, String idVal) {
        eventEl.addContent(new Element("linkingObjectIdentifier", PREMIS_V2_NS)
                .addContent(new Element("linkingObjectIdentifierType", PREMIS_V2_NS).setText(idType))
                .addContent(new Element("linkingObjectIdentifierValue", PREMIS_V2_NS).setText(idVal)));
    }

    public static Model deserializeLogFile(File logFile) throws IOException {
        return createModel(new FileInputStream(logFile), "N-TRIPLE");
    }

    public static Path serializeXMLFile(Path destDir, PID pid, Document doc) throws IOException {
        Path xmlPath = destDir.resolve("uuid_" + pid.getId() + ".xml");
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(doc, outStream);
        return xmlPath;
    }

    public static List<Resource> listEventResources(PID pid, Model model) {
        Resource objResc = model.getResource(pid.getRepositoryPath());
        List<Statement> used = model.listStatements(null, Prov.used, objResc).toList();
        List<Statement> generated = model.listStatements(null, Prov.generated, objResc).toList();

        return Stream.concat(used.stream(), generated.stream())
                .map(Statement::getSubject)
                .collect(toList());
    }

    public static Resource getEventByType(List<Resource> events, Resource type) {
        return events.stream()
                .filter(eResc -> eResc.hasProperty(RDF.type, type))
                .findFirst()
                .orElse(null);
    }

    public static void buildPremisListFile(Path originalLogsPath, Path premisListPath) throws IOException {
        try (Stream<Path> walk = Files.walk(originalLogsPath)) {
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(toList());

            FileUtils.write(premisListPath.toFile(),
                    String.join("\n", result),
                    UTF_8);
        }
    }

    public static Model readTransformedLog(Path outputPath, PID pid) throws IOException{
        Path premisPath = getTransformedPremisPath(outputPath, pid, true);
        return createModel(newInputStream(premisPath), "N-TRIPLE");
    }
}
