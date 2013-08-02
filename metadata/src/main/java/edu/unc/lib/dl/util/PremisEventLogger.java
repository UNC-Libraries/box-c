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
package edu.unc.lib.dl.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.Namespace;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Captures ingest processing of objects. These events are logged on the
 * MD_EVENTS datastream of the appropriate objects. Generally a logger used for
 * a single ingest transaction, but it can log events for all relevant objects
 * in a batch, plus exceptions. The sum of all events for an ingest transaction
 * is also an "ingest report" that can be feed back to the submitter.
 *
 * @author count0
 *
 */
/**
 * @author Gregory Jansen
 *
 */
public class PremisEventLogger {
	public static final String PID_TYPE = "PID";
	public static final Namespace NS = Namespace.getNamespace(JDOMNamespaceUtil.PREMIS_V2_NS.getURI());
	public static final String LOC_EVENT_TYPE_NS = "http://id.loc.gov/vocabulary/preservationEvents/";
	public static final String EXCEPTION_EVENT_TYPE = "exception";
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * This is an enumeration of accepted event types. These should be part of a controlled vocabulary of event types
	 * used in CDR premis.
	 */
	public enum Type {
		CAPTURE("capture"), // The process whereby a repository actively obtains an object.
		COMPRESSION("compression"), // The process of coding data to save storage space or transmission time.
		CREATION("creation"), // The act of creating a new object.
		DEACCESSION("deaccession"), // The process of removing an object from the inventory of a repository.
		DECOMPRESSION("decompression"), // The process of reversing the effects of compression.
		DECRYPTION("decryption"), // The process of converting encrypted data to plaintext.
		DELETION("deletion"), // The process of removing an object from repository storage.
		DIGITAL_SIGNATURE_VALIDATION("digitalSignatureValidation"),
		// The process of determining that a decrypted digital signature matches an expected value.
		// EXCEPTION("exception"), // Any unexpected event that changes the object.
		FIXITY_CHECK("fixityCheck"), // The process of verifying that an object has not been changed in a given period.
		INGESTION("ingestion"), // The process of adding objects to a preservation repository.
		MESSAGE_DIGEST_CALCULATION("messageDigestCalculation"),
		// The process by which a message digest ("hash") is created.
		MIGRATION("migration"), // A transformation of an object creating a version in a more contemporary format.
		NORMALIZATION("normalization"), // A transformation of an object creating a version more conducive to
													// preservation.
		REPLICATION("replication"), // The process of creating a copy of an object that is, bitwise, identical to the
												// original.
		VALIDATION("validation"), // The process of comparing and object with a standard and noting compliance or
											// exceptions.
		VIRUS_CHECK("virusCheck"); // The process of scanning a file for malicious programs.
		private final String id;

		Type(String name) {
			this.id = LOC_EVENT_TYPE_NS + name;
		}

		public String id() {
			return id;
		}
	}

	// private String created = dateFormat.format(new
	// Date(System.currentTimeMillis()));

	private String defaultAgent = null;

	/**
	 * A list of the exception events logged.
	 */
	private final List<Element> exceptionEvents = new ArrayList<Element>();

	/**
	 * A map of pids to the list of events logged for each object.
	 */
	private final Map<PID, Element> pid2EventList = new HashMap<PID, Element>();

	/**
	 * Creates a PREMIS Event Logger.
	 *
	 * @param agent
	 *           the default initiator of events
	 */
	public PremisEventLogger(String defaultAgent) {
		this.defaultAgent = defaultAgent;
	}

	/**
	 * Logs an event, adding supplied XML to an event outcome detail note.
	 *
	 * @param type
	 *           the type of event
	 * @param message
	 *           event message
	 * @param pid
	 *           the PID of the object acted upon
	 * @param outcome
	 *           a string describing the outcome
	 * @param detailNote
	 *           a detail message (can be null)
	 * @param detailExtension
	 *           detail XML element (any schema fits, can be null)
	 * @return the event element
	 */
	public static Element addDetailedOutcome(Element event, String outcome, String detailNote, Element detailExtension) {
		// log the detail xml
		Element eventOutcomeInfo = new Element("eventOutcomeInformation", NS).addContent(new Element("eventOutcome", NS)
				.setText(outcome));
		// outcome info MUST follow eventDetail
		Element d = event.getChild("eventDetail", NS);
		int index = event.indexOf(d);
		if (index + 1 == event.getContentSize()) { // out of range, append
			event.addContent(eventOutcomeInfo);
		} else { // insert
			event.addContent(index + 1, eventOutcomeInfo);
		}

		if (detailNote != null || detailExtension != null) {
			Element eventOutcomeDetail = new Element("eventOutcomeDetail", NS);
			eventOutcomeInfo.addContent(eventOutcomeDetail);

			if (detailNote != null) {
				eventOutcomeDetail.addContent(new Element("eventOutcomeDetailNote", NS).setText(detailNote));
			}
			if (detailExtension != null) {
				eventOutcomeDetail.addContent(new Element("eventOutcomeDetailExtension", NS).addContent(detailExtension));
			}
		}
		return event;
	}

	/**
	 * Appends events for the specified PID to the supplied events element.
	 *
	 * @param pid
	 *           the PID of the object
	 * @param oldXML
	 *           the PREMIS events Element
	 * @return the modified xml element
	 */
	public Element appendLogEvents(PID pid, Element eventsElement) {
		List children = this.pid2EventList.get(pid).cloneContent();// .getChildren("event", NS);
		for (Object child : children) {
			if (child instanceof Element) {
				Element el = (Element) child;
				if ("event".equals(el.getName())) {
					eventsElement.addContent(el);
				}
			}
		}
		return eventsElement;
	}

	/**
	 * Compile a PREMIS XML report of all events logged to this logger.
	 *
	 * @return PREMIS events JDOM Element
	 */
	public Element getAllEvents() {
		Element result = new Element("premis", NS);
		result.setAttribute("version", "2.0");
		for (Element p : this.pid2EventList.values()) {
			result.addContent((Element) p.getChild("object", NS).clone());
		}
		for (Element p : this.pid2EventList.values()) {
			List<Element> events = p.getChildren("event", NS);
			for (Element event : events) {
				result.addContent((Element) event.clone());
			}
		}
		for (Element ex : this.exceptionEvents) {
			result.addContent((Element) ex.clone());
		}
		return result;
	}

	/**
	 * Compile a PREMIS XML report of all events logged for the object.
	 *
	 * @param pid
	 *           the object pid
	 * @return PREMIS events JDOM Element
	 */
	public Element getObjectEvents(PID pid) {
		return this.pid2EventList.get(pid);
	}

	/**
	 * Creates a basic PREMIS Object record for the event log.
	 *
	 * @param pid
	 *           the PID
	 * @return object Element
	 */
	public Element getObjectElement(PID pid) {
		// <object xsi:type="representation">
		// <objectIdentifier>
		// <objectIdentifierType>Fedora PID</objectIdentifierType>
		// <objectIdentifierValue>cdr:1234</objectIdentifierValue>
		// </objectIdentifier>
		// </object>
		Element result = new Element("object", NS);
		result.setAttribute("type", "representation", JDOMNamespaceUtil.XSI_NS);
		Element id = new Element("objectIdentifier", NS);
		id.addContent(new Element("objectIdentifierType", NS).setText(PID_TYPE));
		id.addContent(new Element("objectIdentifierValue", NS).setText(pid.getPid()));
		result.addContent(id);
		return result;
	}
	
	public static Element addSoftwareAgent(Element event, String name, String versionNumber) {
		// add linked agent
		Element lai = new Element("linkingAgentIdentifier", NS);
		lai.addContent(new Element("linkingAgentIdentifierType", NS).setText("Name"));
		lai.addContent(new Element("linkingAgentIdentifierValue", NS).setText(name+" ("+versionNumber+")"));
		lai.addContent(new Element("linkingAgentRole", NS).setText("Software"));
		event.addContent(lai);
		return event;
	}
	
	public static Element addType(Element event, Type type) {
		event.addContent(new Element("eventType", NS).setText(type.id()));
		return event;
	}
	
	public static Element addDateTime(Element event, Date dateTime) {
		event.addContent(new Element("eventDateTime", NS).setText(dateFormat.format(dateTime)));
		return event;
	}
	
	public static Element addLinkingAgentIdentifier(Element event, String type, String value, String role) {
		Element lai = new Element("linkingAgentIdentifier", NS);
		
		if (type != null)
			lai.addContent(new Element("linkingAgentIdentifierType", NS).setText(type));
		
		if (value != null)
			lai.addContent(new Element("linkingAgentIdentifierValue", NS).setText(value));
		
		if (role != null)
			lai.addContent(new Element("linkingAgentRole", NS).setText(role));
		
		event.addContent(lai);
		return event;
	}
	
	public static Element addLinkingObjectIdentifier(Element event, String type, String value, String role) {
		Element loi = new Element("linkingObjectIdentifier", NS);
		
		if (type != null)
			loi.addContent(new Element("linkingObjectIdentifierType", NS).setText(type));
		
		if (value != null)
			loi.addContent(new Element("linkingObjectIdentifierValue", NS).setText(value));
		
		if (role != null)
			loi.addContent(new Element("linkingObjectRole", NS).setText(role));
		
		event.addContent(loi);
		return event;
	}

	public void addEvent(PID pid, Element event) {
		Element events = pid2EventList.get(pid);
		if (events == null) {
			events = new Element("premis", NS);
			events.setAttribute("version", "2.0");
			events.addContent(getObjectElement(pid));
			this.pid2EventList.put(pid, events);
		}
		events.addContent(event);
	}
	
	public Element logEvent(PID pid) {
		Element event = new Element("event", NS);

		// add event identifier UUID
		String uuid = String.format("urn:uuid:%1$s", java.util.UUID.randomUUID());
		event.addContent(new Element("eventIdentifier", NS).addContent(
				new Element("eventIdentifierType", NS).setText("URN")).addContent(
				new Element("eventIdentifierValue", NS).setText(uuid)));

		addEvent(pid, event);
		return event;
	}

	public Element logEvent(Type type, String message, PID pid) {
		long timestamp = System.currentTimeMillis();
		Element event = new Element("event", NS);

		// add event identifier UUID
		String uuid = String.format("urn:uuid:%1$s", java.util.UUID.randomUUID());
		event.addContent(new Element("eventIdentifier", NS).addContent(
				new Element("eventIdentifierType", NS).setText("URN")).addContent(
				new Element("eventIdentifierValue", NS).setText(uuid)));

		// add event type
		event.addContent(new Element("eventType", NS).setText(type.id()));

		// add event date/time
		Date dt = new Date(timestamp);
		event.addContent(new Element("eventDateTime", NS).setText(dateFormat.format(dt)));

		// add event detail message
		event.addContent(new Element("eventDetail", NS).setText(message));

		// add linked agent
		Element lai = new Element("linkingAgentIdentifier", NS);
		lai.addContent(new Element("linkingAgentIdentifierType", NS).setText(PID_TYPE));
		lai.addContent(new Element("linkingAgentIdentifierValue", NS).setText(this.defaultAgent));
		lai.addContent(new Element("linkingAgentRole", NS).setText("Initiator"));
		event.addContent(lai);

		// add linked object (this pid)
		// REMOVED LINK TO SUBJECT PID, it is assumed from main object identifier in the premis document.
		// Element obj = new Element("linkingObjectIdentifier", NS);
		// obj.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
		// obj.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid.getPid()));
		// obj.addContent(new Element("linkingObjectRole", NS).setText("Subject"));
		// event.addContent(obj);

		addEvent(pid, event);
		return event;
	}

	public Element logEvent(Type type, String message, PID pid, String dataStream) {
		Element event = this.logEvent(type, message, pid);
		Element source = new Element("linkingObjectIdentifier", NS);
		source.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
		source.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid.getPid() + "/" + dataStream));
		source.addContent(new Element("linkingObjectRole", NS).setText("Source Data"));
		event.addContent(source);
		return event;
	}

	public Element logDerivationEvent(Type type, String message, PID pid, String sourceDataStream, String destDataStream) {
		Element event = this.logEvent(type, message, pid);
		// log the source and destination datastreams
		// add linked object (this pid)
		Element source = new Element("linkingObjectIdentifier", NS);
		source.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
		source.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid.getPid() + "/" + sourceDataStream));
		source.addContent(new Element("linkingObjectRole", NS).setText("Source Data"));
		event.addContent(source);
		Element dest = new Element("linkingObjectIdentifier", NS);
		dest.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
		dest.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid.getPid() + "/" + destDataStream));
		dest.addContent(new Element("linkingObjectRole", NS).setText("Derived Data"));
		event.addContent(dest);
		return event;
	}

	public void logException(String message, Throwable e) {
		Element event = new Element("event", NS);

		// add event identifier UUID
		String uuid = String.format("urn:uuid:%1$s", java.util.UUID.randomUUID());
		event.addContent(new Element("eventIdentifier", NS).addContent(
				new Element("eventIdentifierType", NS).setText("URN")).addContent(
				new Element("eventIdentifierValue", NS).setText(uuid)));

		// add event type
		event.addContent(new Element("eventType", NS).setText(EXCEPTION_EVENT_TYPE));

		// add event date/time
		Date dt = new Date(System.currentTimeMillis());
		event.addContent(new Element("eventDateTime", NS).setText(dateFormat.format(dt)));
		event.addContent(new Element("eventDetail", NS).setText(e.getMessage()));

		// create event detail note if applicable
		Element detailExtension = null;
		if (e instanceof XMLAttachedException) {
			XMLAttachedException ie = (XMLAttachedException) e;
			detailExtension = ie.getErrorXML();
		}

		// make stack trace
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		// add event outcome
		this.addDetailedOutcome(event, e.getLocalizedMessage(), sw.toString(), detailExtension);

		// push the event into the root element
		this.exceptionEvents.add(event);
	}
}
