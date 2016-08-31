package edu.unc.lib.dl.util;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.rdf.Premis;

public class PremisEventBuilder {
	private String eventId;
	private Model model;
	private PremisLogger premisLogger;
	private Resource premisObjResc;
	
	public PremisEventBuilder(String eventId, Resource eventType, Date date,
			PremisLogger premisLogger) {
		this.eventId = eventId;
		this.premisLogger = premisLogger;
		addEvent(eventType, date);
	}

	private PremisEventBuilder addEvent(Resource eventType, Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Resource premisObjResc = getResource();

		premisObjResc.addProperty(Premis.hasEventType, eventType);
		premisObjResc.addProperty(Premis.hasEventDateTime, dateFormat.format(date));
		
		return this;
	}
	
	public PremisEventBuilder addEventDetail(String message, Object... args) {
		if (args != null) {
			message = MessageFormat.format(message, args);
		}
		Resource premisObjResc = getResource();
		premisObjResc.addProperty(Premis.hasEventDetail, message);
		
		return this;
	}
	
	public PremisEventBuilder addEventDetailOutcomeNote(String detailNote, Object... args) {
		if (args != null) {
			detailNote = MessageFormat.format(detailNote, args);
		}
		
		Resource premisObjResc = getResource();
		premisObjResc.addProperty(Premis.hasEventOutcomeDetailNote, detailNote);

		return this;
	}
	
	public PremisEventBuilder addSoftwareAgent(String agent) {
		Model modelAgent = ModelFactory.createDefaultModel();
		Resource softwareAgent = modelAgent.createResource(Premis.hasEventRelatedAgentExecutor);
		addAgent(Premis.hasEventRelatedAgentExecutor, softwareAgent, "#softwareAgent", agent);
		
		return this;
	}
	
	public PremisEventBuilder addAuthorizingAgent(String agent) {
		Model modelAgent = ModelFactory.createDefaultModel();
		Resource authorizingAgent = modelAgent.createResource(Premis.hasEventRelatedAgentAuthorizor);
		addAgent(Premis.hasEventRelatedAgentAuthorizor, authorizingAgent, "#authorizingAgent", agent);
		
		return this;
	}
	
	public PremisEventBuilder addDerivative(String sourceDataStream, String destDataStream) {
		Resource premisObjResc = getResource();
		
		premisObjResc.addProperty(Premis.hasAgentName, sourceDataStream);
		premisObjResc.addProperty(Premis.hasAgentType, "Source Data");
		premisObjResc.addProperty(Premis.hasAgentName, destDataStream);
		premisObjResc.addProperty(Premis.hasAgentType, "Derived Data");
		
		return this;
	}
	
	public PremisEventBuilder addAgent(Property role, Resource type, String agentId, String name) {
		Resource premisObjResc = getResource();
		Resource linkingAgentInfo = model.createResource(agentId);

		linkingAgentInfo.addProperty(Premis.hasAgentType, type);
		linkingAgentInfo.addProperty(Premis.hasAgentName, name);
		premisObjResc.addProperty(role, linkingAgentInfo);
		
		return this;
	}
	
	public Resource create() {
		return getResource();
	}
	
	public Resource write() {
		Resource resource = getResource();
		premisLogger.writeEvent(resource);
		return resource;
	}
	
	private Resource getResource() {
		if (premisObjResc != null) {
			return premisObjResc;
		}
		
		model = getModel();
		premisObjResc = model.createResource(this.premisLogger.cdrEventURI + this.eventId);
		
		return premisObjResc;
	}
	
	private Model getModel() {
		if (model != null) {
			return model;
		}
		
		return ModelFactory.createDefaultModel();
	}
}
