package edu.unc.lib.dl.update;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;

import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.PremisEventLogger.Type;

/**
 * Filter which performs update operations on an MD_DESCRIPTIVE MODS datastream and validates it.
 * @author bbpennel
 *
 */
public class MODSUIPFilter implements UIPUpdateFilter {
	private static Logger log = Logger.getLogger(MODSUIPFilter.class);

	private String datastreamName = Datastream.MD_DESCRIPTIVE.getName();
	private SchematronValidator schematronValidator;

	@Override
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
		// Do not apply filter unless the mods ds is being targeted.
		if (!(uip.getIncomingData().containsKey(datastreamName) || uip.getModifiedData().containsKey(datastreamName)))
			return uip;

		// Only run this filter for metadata update requests
		if (!(uip instanceof MetadataUIP))
			return uip;
		MetadataUIP metadataUIP = (MetadataUIP) uip;

		log.debug("Performing MODS filter operation " + uip.getOperation().name() + " on " + uip.getPID().getPid());

		Element newModified = null;
		
		switch (uip.getOperation()) {
			case REPLACE:
				newModified = performReplace(metadataUIP);
				break;
			case ADD:
				newModified = performAdd(metadataUIP);
				break;
			case UPDATE:
				// Doing add for update since the schema does not allow a way to indicate a tag should replace another
				newModified = performAdd(metadataUIP);
				break;
		}
		
		if (newModified != null){
			//Validate the new mods before storing
			validate(uip, newModified);
			metadataUIP.getModifiedData().put(datastreamName, newModified);
		}

		return uip;
	}

	private Element performReplace(MetadataUIP uip) throws UIPException {
		return (Element) uip.getIncomingData().get(datastreamName).clone();
	}

	private Element performAdd(MetadataUIP uip) throws UIPException {
		Element incoming = uip.getIncomingData().get(datastreamName);
		if (incoming == null)
			return null;

		Element modified = uip.getModifiedData().get(datastreamName);
		Element original = uip.getOriginalData().get(datastreamName);

		Element newModified = null;

		if (modified == null) {
			// If there is no original or modified data, than return the incoming as new modified
			if (original == null) {
				return (Element)incoming.clone();
			} else {
				// Set the base for the new modified object to the original data
				newModified = (Element) original.clone();
			}
		} else {
			// Use the previous modified data
			newModified = (Element)modified.clone();
		}

		// Clone all the child elements of the incoming mods element
		@SuppressWarnings("unchecked")
		List<Element> incomingElements = (List<Element>) incoming.getChildren();
		// Add all the incoming element children to the base modified object
		for (Element incomingElement : incomingElements) {
			newModified.addContent((Element) incomingElement.clone());
		}
		
		return newModified;
	}
	
	private void validate(UpdateInformationPackage uip, Element mods) throws UIPException {
		Document svrl = schematronValidator.validate(new JDOMSource(mods), "vocabularies-mods");
		String message = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
		Element event = uip.getEventLogger().logEvent(Type.VALIDATION, message, uip.getPID(), "MD_DESCRIPTIVE");
		if (!schematronValidator.hasFailedAssertions(svrl)) {
			uip.getEventLogger().addDetailedOutcome(event, "MODS is valid",
					"The supplied MODS metadata meets CDR vocabulary requirements.", null);
		} else {
			uip.getEventLogger().addDetailedOutcome(event, "MODS is not valid",
					"The supplied MODS metadata does not meet CDR vocabulary requirements.", svrl.detachRootElement());
			throw new UIPException("The supplied MODS metadata did not meet requirements.");
		}
	}

}
