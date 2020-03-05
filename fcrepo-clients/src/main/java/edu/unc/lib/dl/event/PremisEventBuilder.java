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

import static java.text.Normalizer.Form.NFD;
import static java.util.Locale.ENGLISH;

import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.PremisAgentType;
import edu.unc.lib.dl.rdf.Prov;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Builder for creating a PREMIS RDF event.
 *
 * @author lfarrell
 *
 */
public class PremisEventBuilder {

    private PID eventPid;
    private Model model;
    private PremisLogger premisLogger;
    private Resource premisObjResc;

    public PremisEventBuilder(PID eventSubject, PID eventPid, Resource eventType, Date date,
            PremisLogger premisLogger) {
        this.eventPid = eventPid;
        this.premisLogger = premisLogger;
        addEvent(eventSubject, eventType, date);
    }

    /**
     * Adds basic information required for all events
     *
     * @param eventType
     * @param date
     * @return
     */
    private PremisEventBuilder addEvent(PID eventSubject, Resource eventType, Date date) {
        Resource premisObjResc = getResource();

        Model logModel = getModel();
        Resource eventSubjectResc = logModel.getResource(eventSubject.getRepositoryPath());
        if (Premis.Ingestion.equals(eventType)
                || Premis.Creation.equals(eventType)) {
            eventSubjectResc.addProperty(Prov.wasGeneratedBy, premisObjResc);
        } else {
            eventSubjectResc.addProperty(Prov.wasUsedBy, premisObjResc);
        }
        premisObjResc.addProperty(RDF.type, eventType);
        premisObjResc.addProperty(DCTerms.date,
                DateTimeUtil.formatDateToUTC(date), XSDDatatype.XSDdateTime);

        return this;
    }

    /**
     * Add an event detail property to this event
     *
     * @param message
     *            The detail message for this event.
     * @param args
     *            Optional parameters that should be formatted into the message,
     *            using String.format syntax.
     * @return this event builder
     */
    public PremisEventBuilder addEventDetail(String message, Object... args) {
        if (args != null && args.length > 0) {
            message = MessageFormat.format(message, args);
        }
        Resource premisObjResc = getResource();
        premisObjResc.addProperty(Premis.note, message);

        return this;
    }

    /**
     * Add an event detail outcome note property to this event
     *
     * @param detailNote
     *            The message for this outcome detail
     * @param args
     *            Optional parameters that should be formatted into the message,
     *            using String.format syntax.
     * @return this event builder
     */
    public PremisEventBuilder addEventDetailOutcomeNote(String detailNote, Object... args) {
        if (args != null) {
            detailNote = MessageFormat.format(detailNote, args);
        }

        Resource premisObjResc = getResource();
        premisObjResc.addProperty(Premis.hasEventOutcomeDetailNote, detailNote);

        return this;
    }

    /**
     * Add a related software agent to this event
     *
     * @param agent Identifier for the agent
     * @return this event builder
     */
    public PremisEventBuilder addSoftwareAgent(String agent) {
        addAgent(Premis.hasEventRelatedAgentExecutor, PremisAgentType.Software,
                toSlug("#softwareAgent", agent), agent);

        return this;
    }

    /**
     * Add a related authorizing agent to this event
     *
     * @param agent identifier for the agent
     * @return this event builder
     */
    public PremisEventBuilder addAuthorizingAgent(String agent) {
        addAgent(Premis.hasEventRelatedAgentAuthorizor, PremisAgentType.Person,
                toSlug("#authorizingAgent", agent), agent);

        return this;
    }

    public PremisEventBuilder addImplementorAgent(String agent) {
        addAgent(Premis.hasEventRelatedAgentImplementor, PremisAgentType.Person,
                toSlug("#implementorAgent", agent), agent);

        return this;
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /*
     * Convert an agent plus a prefix into a slug for inclusion in URI of the agent to prevent collisions
     * in the case of multiple agents of the same type
     */
    private static String toSlug(String prefix, String agent) {
      String nowhitespace = WHITESPACE.matcher(agent).replaceAll("-");
      String normalized = Normalizer.normalize(nowhitespace, NFD);
      String slug = NONLATIN.matcher(normalized).replaceAll("");
      return prefix + "-" + slug.toLowerCase(ENGLISH);
    }

    /**
     * Add details describing the creation of a derivative datastream
     *
     * @param sourceDataStream
     *            The identifier of source datastream
     * @param destDataStream
     *            The identifier of the datastream derived from the source.
     * @return this event builder
     */
    public PremisEventBuilder addDerivative(String sourceDataStream, String destDataStream) {
        Resource premisObjResc = getResource();

        premisObjResc.addProperty(Premis.hasAgentName, sourceDataStream);
        premisObjResc.addProperty(Premis.hasAgentType, "Source Data");
        premisObjResc.addProperty(Premis.hasAgentName, destDataStream);
        premisObjResc.addProperty(Premis.hasAgentType, "Derived Data");

        return this;
    }

    /**
     * Add an agent to this event
     *
     * @param role
     * @param type
     * @param agentId
     * @param name
     * @return
     */
    private PremisEventBuilder addAgent(Property role, Resource type, String agentId, String name) {
        Resource premisObjResc = getResource();
        Resource linkingAgentInfo = model.createResource(eventPid.getRepositoryPath() + agentId);

        linkingAgentInfo.addProperty(Premis.hasAgentType, type);
        linkingAgentInfo.addProperty(Premis.hasAgentName, name);
        premisObjResc.addProperty(role, linkingAgentInfo);

        return this;
    }

    /**
     * Finalize this builder by retrieving the created event resource
     *
     * @return
     */
    public Resource create() {
        return getResource();
    }

    /**
     * Finalize this builder by pushing the built event back to the log
     *
     * @return
     */
    public Resource write() {
        Resource resource = getResource();
        premisLogger.writeEvents(resource);
        return resource;
    }

    /**
     * Finalize this builder by pushing the built event back to the log
     * and then closing the logger
     *
     * @return
     */
    public Resource writeAndClose() {
        Resource resc = write();
        premisLogger.close();
        return resc;
    }

    private Resource getResource() {
        if (premisObjResc != null) {
            return premisObjResc;
        }

        model = getModel();
        premisObjResc = model.createResource(eventPid.getRepositoryPath());

        return premisObjResc;
    }

    private Model getModel() {
        if (model != null) {
            return model;
        }

        return ModelFactory.createDefaultModel();
    }
}
