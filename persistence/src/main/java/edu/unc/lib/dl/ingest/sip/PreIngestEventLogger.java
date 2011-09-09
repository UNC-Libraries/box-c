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
/**
 *
 */
package edu.unc.lib.dl.ingest.sip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;

/**
 * This class is used to record events that occur on the SIP prior to ingest.
 * These events apply to all the objects in the SIP equally. Individual object
 * IDs are not recorded in this logger, but will be added later after PIDs are
 * assigned when events are moved to the AIP.
 *
 * PID have not been assigned yet when these events are logged. This is a
 * convenience class and various SIP implementations may expose it to SIP
 * creators or not.
 *
 * SIP implementations may provide other means for more granular pre-ingest
 * events to be passed to the AIP, for example a metadata block in METS.
 *
 * @author Gregory Jansen
 *
 */
public class PreIngestEventLogger {
    private static final Namespace NS = PremisEventLogger.NS;
    private static final SimpleDateFormat sdf = PremisEventLogger.dateFormat;
    // private static final String PID_TYPE = PremisEventLogger.PID_TYPE;
    private List<Element> events = new ArrayList<Element>();
//    private static Element objectStub = null;

//    static {
//	objectStub = new Element("linkingObjectIdentifier", NS);
//	objectStub.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
//	// objectStub.addContent(new Element("linkingObjectIdentifierValue",
//	// NS).setText(pid.getURI()));
//	objectStub.addContent(new Element("linkingObjectRole", NS).setText("Subject"));
//    }

    /**
     * Generates a List of pre-ingest event Elements for the given PID.
     *
     * @param pid
     * @return a list of the events for this PID prior to ingest
     */
    List<Element> getEvents(PID pid) {
	List<Element> result = new ArrayList<Element>();
	for (Element e : events) {
	    Element out = (Element) e.clone();
	    //Element linkingObject = (Element) objectStub.clone();
	    //linkingObject.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid.getPid()));
	    //out.addContent(linkingObject);
	    result.add(out);
	}
	return result;
    }

    public boolean hasEvents() {
	return (events.size() > 0);
    }

    /**
     * Add a pre-ingest virus scan event.
     *
     * @param date
     *            the date of the scan
     * @param software
     *            the name and version of the software used
     * @param person
     *            the person who initiated the scan
     */
    public Element addVirusScan(Date date, String software, String person) {
	Element scan = makeEvent(PremisEventLogger.Type.VALIDATION, "virus scan", person, date);
	if (software != null) {
	    addAgent(scan, software, "Name", "Software");
	}
	events.add(scan);
	return scan;
    }

    /**
     * Add a MD5 check calculation event. (use the earliest checksum calculation
     * available)
     *
     * @param date
     *            the date of the MD5 calculation
     * @param software
     *            the software name and version used to calculate the checksum
     * @param person
     *            the person who initiated the checksum calculation
     */
    public Element addMD5ChecksumCalculation(Date date, String software, String person) {
	Element event = makeEvent(PremisEventLogger.Type.VALIDATION, "MD5 checksum calculated", person, date);
	if (software != null) {
	    addAgent(event, software, "Name", "Software");
	}
	events.add(event);
	return event;
    }

    /**
     * Add a SIP creation event, usually the act of creating a manifest. This is
     * not the same as the ingest event.
     *
     * @param date
     *            the date the SIP was created
     * @param software
     *            the software used or null if none
     * @param person
     *            the person who created the SIP
     */
    public Element addSIPCreation(String date, String software, String person) {
	Element event = makeEvent(PremisEventLogger.Type.CREATION, "SIP created", person, date);
	if (software != null) {
	    addAgent(event, software, "Name", "Software");
	}
	events.add(event);
	return event;
    }

    /**
     * Add a generic pre-ingest event.
     *
     * @param type
     *            the type of event
     * @param agent
     *            the agent who initiated the event
     * @param message
     *            a message describing the event
     * @param date
     *            the date of the event
     */
    public void addEvent(PremisEventLogger.Type type, String agent, String message, Date date) {
	Element event = makeEvent(type, message, agent, date);
	events.add(event);
    }

    /**
     * Add a linking agent to the event.
     *
     * @param event
     *            the event Element
     * @param agentID
     *            the ID of the agent
     * @param idType
     *            the identifier type
     * @param role
     *            the role of the agent
     */
    public void addAgent(Element event, String agentID, String idType, String role) {
	Element lai = new Element("linkingAgentIdentifier", NS);
	lai.addContent(new Element("linkingAgentIdentifierType", NS).setText(idType));
	lai.addContent(new Element("linkingAgentIdentifierValue", NS).setText(agentID));
	lai.addContent(new Element("linkingAgentRole", NS).setText(role));
	event.addContent(lai);
    }

    /**
     * Constructs a template event.
     *
     * @param type
     * @param message
     * @param operatorName
     * @param date
     * @return
     */
    private Element makeEvent(Type type, String message, String operatorName, Date date) {
	return makeEvent(type, message, operatorName, sdf.format(date));
    }

    /**
     * Constructs a template event.
     *
     * @param type
     * @param message
     * @param operatorName
     * @param iso8601Date
     * @return
     */
    private Element makeEvent(Type type, String message, String operatorName, String iso8601Date) {
	Element event = new Element("event", NS);

	// add event identifier UUID
	String uuid = String.format("urn:uuid:%1$s", java.util.UUID.randomUUID());
	event.addContent(new Element("eventIdentifier", NS).addContent(
			new Element("eventIdentifierType", NS).setText("URN")).addContent(
			new Element("eventIdentifierValue", NS).setText(uuid)));

	// add event type
	event.addContent(new Element("eventType", NS).setText(type.id()));

	// add event date/time
	event.addContent(new Element("eventDateTime", NS).setText(iso8601Date));

	// add event detail message
	event.addContent(new Element("eventDetail", NS).setText(message));

	// add linked agent
	if (operatorName != null) {
	    Element lai = new Element("linkingAgentIdentifier", NS);
	    lai.addContent(new Element("linkingAgentIdentifierType", NS).setText("Name"));
	    lai.addContent(new Element("linkingAgentIdentifierValue", NS).setText(operatorName));
	    lai.addContent(new Element("linkingAgentRole", NS).setText("Initiator"));
	    event.addContent(lai);
	}
	return event;
    }

}
