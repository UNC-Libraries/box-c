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

import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATE_MODS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.clamav;
import static edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent.depositService;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;

import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Populates a PREMIS 3 logger with events transformed from an XML PREMIS 2 log for one object.
 *
 * @author bbpennel
 *
 */
public class DepositRecordPremisToRdfTransformer {
    private static final Logger log = getLogger(DepositRecordPremisToRdfTransformer.class);

    private static final Pattern NORMALIZE_FORMAT_PATTERN = Pattern.compile("Normalized deposit package from ([^ ]+) to.*");

    private PID pid;
    private PremisLogger premisLogger;
    private Document doc;

    public DepositRecordPremisToRdfTransformer(PID pid, PremisLogger premisLogger, Document doc) {
        this.pid = pid;
        this.premisLogger = premisLogger;
        this.doc = doc;
    }

    public void transform() {
        List<Element> eventEls = doc.getRootElement().getChildren("event", PREMIS_V2_NS);

        for (Element eventEl: eventEls) {
            List<String> eventTypes = getEventTypes(eventEl);
            if (eventTypes.isEmpty()) {
                log.error("No type present on event for record {}", pid);
                continue;
            }

            if (eventTypes.contains(VIRUS_CHECK_TYPE)) {
                addVirusCheckEvent(eventEl);
            } else if (eventTypes.contains(Premis2Constants.INGESTION_TYPE)) {
                addIngestionEvent(eventEl);
            } else if (eventTypes.contains(Premis2Constants.NORMALIZATION_TYPE)) {
                transformNormalizationEvent(eventEl);
            } else if (eventTypes.contains(Premis2Constants.VALIDATION_TYPE)) {
                addValidationEvent(eventEl);
            } else {
                log.error("Unknown event types for {}: {}", pid, eventTypes);
            }
        }
    }

    private void addVirusCheckEvent(Element eventEl) {
        createEventBuilder(Premis.VirusCheck, eventEl)
            .addEventDetail(getEventDetail(eventEl))
            .addSoftwareAgent(clamav.getFullname())
            .write();
    }

    private void addIngestionEvent(Element eventEl) {
        String eventDetail = getEventDetail(eventEl);
        if (eventDetail == null) {
            log.error("No event details for ingestion event on {}", pid);
            return;
        }

        if (!eventDetail.contains("ingested as PID")) {
            log.error("Unknown deposit validation event for {}, with detail: {}", pid, eventDetail);
            return;
        }

        // ingested as PID
        createEventBuilder(Premis.Ingestion, eventEl)
            .addEventDetail(getEventDetail(eventEl))
            .addSoftwareAgent(depositService.getFullname())
            .write();
    }

    private void addValidationEvent(Element eventEl) {
        String eventDetail = getEventDetail(eventEl);
        if (eventDetail == null) {
            log.error("No event details for validation event on {}", pid);
            return;
        }

        String agent = getLinkingAgent(eventEl);
        if (VALIDATE_MODS_AGENT.equals(agent)) {
            log.info("Ignoring MODS validation event on {}", pid);
            return;
        }

        if (!eventDetail.contains("METS")) {
            log.error("Unknown deposit validation event for {}, with detail: {}", pid, eventDetail);
            return;
        }

        createEventBuilder(Premis.Validation, eventEl)
            .addEventDetail("METS schema validated")
            .addSoftwareAgent(depositService.getFullname())
            .write();
    }

    private void transformNormalizationEvent(Element eventEl) {
        String eventDetail = getEventDetail(eventEl);
        if (eventDetail == null) {
            log.error("No event details for normalization event on {}", pid);
            return;
        }

        // PID assignment normalization jobs become info package creation
        if (eventDetail.contains("Assigned PID")) {
            createEventBuilder(Premis.InformationPackageCreation, eventEl)
                .addEventDetail(eventDetail)
                .addSoftwareAgent(depositService.getFullname())
                .write();
            return;
        }

        // Check if it is a format normalization event
        Matcher matcher = NORMALIZE_FORMAT_PATTERN.matcher(eventDetail);
        if (!matcher.matches()) {
            log.error("Unknown deposit normalization event for {}, with detail: {}", pid, eventDetail);
            return;
        }

        String format = matcher.group(1);
        createEventBuilder(Premis.Ingestion, eventEl)
            .addEventDetail("ingested as format: " + format)
            .addSoftwareAgent(depositService.getFullname())
            .write();
    }

    private List<String> getEventTypes(Element eventEl) {
        return eventEl.getChildren("eventType", PREMIS_V2_NS).stream()
                .map(Element::getTextTrim)
                .collect(toList());
    }

    private Date getEventDateTime(Element eventEl) {
        String eventDateTime = eventEl.getChildTextTrim("eventDateTime", PREMIS_V2_NS);
        if (eventDateTime == null) {
            log.warn("No datetime for event on object {}", pid);
            return null;
        }
        return DateTimeUtil.parseUTCToDateTime(eventDateTime).toDate();
    }

    private String getEventDetail(Element eventEl) {
        return eventEl.getChildTextTrim("eventDetail", PREMIS_V2_NS);
    }

    private PID getEventPid(Element eventEl) {
        String idVal = eventEl.getChild("eventIdentifier", PREMIS_V2_NS)
                .getChildTextTrim("eventIdentifierValue", PREMIS_V2_NS);
        idVal = idVal.replaceFirst("urn:uuid:", "");

        String eventUrl = URIUtil.join(pid.getRepositoryPath(), idVal);
        return PIDs.get(eventUrl);
    }

    private String getLinkingAgent(Element eventEl) {
        return eventEl.getChild("linkingAgentIdentifier", PREMIS_V2_NS)
            .getChildTextTrim("linkingAgentIdentifierValue", PREMIS_V2_NS);
    }

    private PremisEventBuilder createEventBuilder(Resource eventTypeResc, Element eventEl) {
        Date dateTime = getEventDateTime(eventEl);
        PID eventPid = getEventPid(eventEl);
        return premisLogger.buildEvent(eventPid, eventTypeResc, dateTime);
    }
}
