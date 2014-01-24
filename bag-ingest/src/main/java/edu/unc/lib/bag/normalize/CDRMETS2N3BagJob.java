package edu.unc.lib.bag.normalize;

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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.bag.AbstractBagJob;
import edu.unc.lib.bag.normalize.METSGraphExtractor.FilePathFunction;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.METSParseException;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class CDRMETS2N3BagJob extends AbstractBagJob {
	private static final Logger log = LoggerFactory.getLogger(CDRMETS2N3BagJob.class);
	
	public CDRMETS2N3BagJob() {
		super();
	}
	
	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	@Autowired
	private SchematronValidator schematronValidator = null;
	
	@Autowired
	private Schema metsSipSchema = null;
	
	private File getMETSFile() {
		return new File(getBagDirectory(), "mets.xml");
	}

	public Schema getMetsSipSchema() {
		return metsSipSchema;
	}

	public void setMetsSipSchema(Schema metsSipSchema) {
		this.metsSipSchema = metsSipSchema;
	}

	public CDRMETS2N3BagJob(File bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	@Override
	public void run() {
		validateMETS();
		List<String> errors = schematronValidator.validateReportErrors(new StreamSource(getMETSFile()), METSProfile.CDR_SIMPLE.getName());
		if(errors != null && errors.size() > 0) {
			String msg = MessageFormat.format("METS is not valid with respect to profile: {}", METSProfile.CDR_SIMPLE.getName());
			StringBuilder details = new StringBuilder();
			for(String error : errors) {
				details.append(error).append('\n');
			}
			failDeposit(Type.VALIDATION, msg, details.toString());
		}
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs

		Model model = ModelFactory.createDefaultModel();
		METSGraphExtractor extractor = new METSGraphExtractor(mets, this.getDepositPID());
		extractor.addArrangement(model);
		extractor.addFileAssociations(model);
		extractor.addAccessControls(model);
		final File modsFolder = new File(getBagDirectory(), "description");
		modsFolder.mkdir();
		extractor.saveDescriptions(new FilePathFunction() {
			@Override
			public String getPath(String piduri) {
				String uuid = new PID(piduri).getUUID();
				return new File(modsFolder, uuid+".xml").getAbsolutePath();
			}
		});
		saveModel(model, "everything.n3");
		// extract MODS files
		
		// addN3PackagingType();
	}
	
	private void saveModel(Model model, String tagfilepath) {
		File arrangementFile = new File(this.getBagDirectory(), tagfilepath);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(arrangementFile);
			model.write(fos, "N-TRIPLE");
		} catch(IOException e) {
			throw new Error("Cannot open file "+arrangementFile, e);
		} finally {
			try {
				fos.close();
			} catch (IOException ignored) {}
		}
	}

	private void assignPIDs(Document mets) {
		int count = 0;
		Iterator divs = mets.getDescendants(new Filter() {
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
			count++;
		}
		recordEvent(Type.NORMALIZATION, "Assigned {0,choice,1#PID|2#PIDs} to {0,choice,1#one object|2#{0,number} objects} ", count);
	}

	private Document loadMETS() {
		Document mets = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			mets = builder.build(getMETSFile());
		} catch (Exception e) {
			failDeposit(e, Type.NORMALIZATION, "Unexpected error parsing METS file: {0}", getMETSFile().getAbsolutePath());
		}
		return mets;
	}
	
	private void saveMETS(Document mets) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(getMETSFile());
			new XMLOutputter().output(mets, fos);
		} catch(Exception e) {
			failDeposit(e, Type.NORMALIZATION, "Unexpected error saving METS: {0}", getMETSFile());
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void validateMETS() {
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
			failDeposit(handler, Type.VALIDATION, "METS is not valid with respect to schemas");
		} catch (IOException e) {
			failDeposit(e, Type.VALIDATION, "Cannot parse METS file: {0}", getMETSFile());
		}
		recordEvent(Type.VALIDATION, "METS schema(s) validated");
	}

}
