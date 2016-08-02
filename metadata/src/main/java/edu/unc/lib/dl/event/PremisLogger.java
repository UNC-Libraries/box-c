package edu.unc.lib.dl.event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.PremisEventBuilder;

public class PremisLogger {
	private static final Logger log = LoggerFactory
			.getLogger(PremisLogger.class);
	public File file;
	public PID pid;
	public PremisEventBuilder premisEventBuilder;
	public Model model;
	public String eventId;
	public String cdrEventURI = "http://cdr.lib.unc.edu/event/";
	
	public PremisLogger(PID pid, File file) {
		this.pid = pid;
		this.file = file;
		this.eventId = generateUUID();
	}
	
	/**
	 * Allows for an arbitrary timestamp to be set for a premis event
	 * @param eventType
	 * @return PremisEventBuilder
	 */
	public PremisEventBuilder buildEvent(Resource eventType, Date date) {
		if (date == null) {
			date = new Date();
		}
		
		return new PremisEventBuilder(this.eventId, eventType, date, this);
	}
	
	/**
	 * Returns an instance of buildEvent with the timestamp automatically set to the current time
	 * @param eventType
	 * @return PremisEventBuilder
	 */
	public PremisEventBuilder buildEvent(Resource eventType) {
		return new PremisEventBuilder(this.eventId, eventType, new Date(), this);
	}
	
	public PremisLogger writeEvent(Model premisBuilder) {
		Model objModel = objectModel();
		Model writeModel = modelMerge(objModel, premisBuilder);
		
		try (FileOutputStream rdfFile = new FileOutputStream(this.file)) {
			RDFDataMgr.write(rdfFile, writeModel, RDFFormat.TURTLE_PRETTY);
		} catch (IOException e) {
			log.debug("Failed to serialize properties for object {} for the following reason {}", 
				this.pid.getUUID(), e.getMessage());
		}
		
		return this;
	}
	
	public Model objectModel() {
		Model model = getModel();
		Resource premisObjResc = model.createResource(this.pid.getURI());
		
		premisObjResc.addProperty(Premis.hasEvent, cdrEventURI + this.eventId);
		
		return model;
	}
	
	public Model modelMerge(Model objModel, Model eventModel) {
		Model mergedModel = objModel.add(eventModel);
		return mergedModel;
	}
	
	public Model getModel() {
		model = ModelFactory.createDefaultModel();
		
		if (this.file.exists()) {
			InputStream in = FileManager.get().open(this.file.getAbsolutePath());
			model.read(in, null, "TURTLE");
		}
		
		return model;
	}
	
	private String generateUUID() {
		return UUID.randomUUID().toString();
	}
}
