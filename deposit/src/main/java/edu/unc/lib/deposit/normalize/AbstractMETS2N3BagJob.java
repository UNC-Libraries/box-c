package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.METSParseException;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.METSProfile;
import edu.unc.lib.dl.xml.NamespaceConstants;

public abstract class AbstractMETS2N3BagJob extends AbstractDepositJob {

	private static final Logger log = LoggerFactory.getLogger(AbstractMETS2N3BagJob.class);
	
	@Autowired
	protected SchematronValidator schematronValidator = null;
	@Autowired
	private Schema metsSipSchema = null;

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	public AbstractMETS2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public AbstractMETS2N3BagJob() {
		super();
	}

	protected File getMETSFile() {
		File dataDir = new File(getDepositDirectory(), "data");
		File result = new File(dataDir, "mets.xml");
		if(!result.exists()) {
			result = new File(dataDir, "METS.xml");
		}
		if(!result.exists()) {
			result = new File(dataDir, "METS.XML");
		}
		if(!result.exists()) {
			result = new File(dataDir, getDepositStatus().get(DepositField.fileName.name()));
		}
		return result;
	}

	public Schema getMetsSipSchema() {
		return metsSipSchema;
	}

	public void setMetsSipSchema(Schema metsSipSchema) {
		this.metsSipSchema = metsSipSchema;
	}

	protected void assignPIDs(Document mets) {
		int count = 0;
		@SuppressWarnings("unchecked")
		Iterator<Element> divs = (Iterator<Element>)mets.getDescendants(new Filter() {
			private static final long serialVersionUID = 691336623641275783L;
			@Override
			public boolean matches(Object obj) {
				if(!Element.class.isInstance(obj)) return false;
				Element e = (Element)obj;
				if(NamespaceConstants.METS_URI.equals(e.getNamespaceURI()) && "div".equals(e.getName())) {
					String cids = e.getAttributeValue("CONTENTIDS");
					if(cids == null) return true;
					if(!cids.contains("info:fedora/")) return true;
					return false;
				}
				return false;
			}
		});
		while(divs.hasNext()) {
			UUID uuid = UUID.randomUUID();
			PID pid = new PID("uuid:"+uuid.toString());
			Element div = (Element)divs.next();
			String cids = div.getAttributeValue("CONTENTIDS");
			if(cids == null) {
				div.setAttribute("CONTENTIDS", pid.getURI());
			} else {
				StringBuilder sb = new StringBuilder(pid.getURI());
				for(String s : cids.split("\\s")) {
					sb.append(" ").append(s);
				}
				div.setAttribute("CONTENTIDS", sb.toString());
			}
			Element pidEvent = getEventLog().logEvent(Type.NORMALIZATION, "Assigned PID to object defined in a METS div", pid);
			appendDepositEvent(pid, pidEvent);
			count++;
		}
		recordDepositEvent(Type.NORMALIZATION, "Assigned {0,choice,1#PID|2#PIDs} to {0,choice,1#one object|2#{0,number} objects} ", count);
	}

	protected Document loadMETS() {
		Document mets = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			mets = builder.build(getMETSFile());
		} catch (Exception e) {
			failJob(e, Type.NORMALIZATION, "Unexpected error parsing METS file: {0}", getMETSFile().getAbsolutePath());
		}
		return mets;
	}

	protected void saveMETS(Document mets) {
		try(FileOutputStream fos = new FileOutputStream(getMETSFile())) {
			new XMLOutputter().output(mets, fos);
		} catch(Exception e) {
			failJob(e, Type.NORMALIZATION, "Unexpected error saving METS: {0}", getMETSFile());
		}
	}

	protected void validateMETS() {
		if(!getMETSFile().exists()) {
			failJob(Type.VALIDATION, "Cannot find a METS file", "A METS was not found in the expected locations: mets.xml, METS.xml or METS.XML");
		}
		Validator metsValidator = getMetsSipSchema().newValidator();
		METSParseException handler = new METSParseException(
				"There was a problem parsing METS XML.");
		metsValidator.setErrorHandler(handler);
		try {
			metsValidator.validate(new StreamSource(getMETSFile()));
		} catch (SAXException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			failJob(handler, Type.VALIDATION, "METS is not valid with respect to schemas");
		} catch (IOException e) {
			failJob(e, Type.VALIDATION, "Cannot parse METS file: {0}", getMETSFile());
		}
		recordDepositEvent(Type.VALIDATION, "METS schema(s) validated");
	}

	protected void validateProfile(METSProfile profile) {
		List<String> errors = schematronValidator.validateReportErrors(new StreamSource(getMETSFile()), profile.getName());
		if(errors != null && errors.size() > 0) {
			String msg = MessageFormat.format("METS is not valid with respect to profile: {0}", profile.getName());
			StringBuilder details = new StringBuilder();
			for(String error : errors) {
				details.append(error).append('\n');
			}
			failJob(Type.VALIDATION, msg, details.toString());
		}
	}

}