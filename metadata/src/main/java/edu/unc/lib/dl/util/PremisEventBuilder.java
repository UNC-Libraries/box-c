package edu.unc.lib.dl.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;


import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.rdf.Premis;

public class PremisEventBuilder {
	private String eventId;
	private Resource eventType;
	private Date date;
	private Model model;
	private PremisLogger premisLogger;
	private Resource premisObjResc;
	
	public PremisEventBuilder(String eventId, Resource eventType, Date date,
			PremisLogger premisLogger) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.date = date;
		this.premisLogger = premisLogger;
		addEvent();
	}

	private PremisEventBuilder addEvent() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Resource premisObjResc = getResource();

		premisObjResc.addProperty(Premis.hasEventType, this.eventType);
		premisObjResc.addProperty(Premis.hasEventDateTime, dateFormat.format(this.date));
		
		return this;
	}
	
	public PremisEventBuilder addEventDetail(String message) {
		Resource premisObjResc = getResource();
		premisObjResc.addProperty(Premis.hasEventDetail, message);
		
		return this;
	}
	
	public PremisEventBuilder addSoftwareAgent(String name, String versionNumber) {	
		Resource premisObjResc = getResource();
		
		premisObjResc.addProperty(Premis.hasAgentName, name+" ("+versionNumber+")");
		premisObjResc.addProperty(Premis.hasAgentType, "Software");

		return this;
	}
	
	public Model create() {
		return getModel();
	}
	
	private Resource getResource() {
		if (premisObjResc != null) {
			return premisObjResc;
		}
		
		model = getModel();
		premisObjResc = model.createResource(this.premisLogger.cdrEventURI + this.eventId);
		
		return premisObjResc;
	}
	
	public Model getModel() {
		if (model != null) {
			return model;
		}
		
		return ModelFactory.createDefaultModel();
	}
}
