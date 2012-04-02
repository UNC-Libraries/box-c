package edu.unc.lib.dl.update;

import org.apache.log4j.Logger;
import org.jdom.Element;

import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class RELSEXTUIPFilter extends MetadataUIPFilter {
	private static Logger log = Logger.getLogger(RELSEXTUIPFilter.class);
	
	private final String datastreamName = Datastream.RELS_EXT.getName();
	
	@Override
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
	// Only run this filter for metadata update requests
		if (uip == null || !(uip instanceof MetadataUIP))
			return uip;

		// Do not apply filter unless the mods ds is being targeted.
		if (!(uip.getIncomingData().containsKey(datastreamName) || uip.getModifiedData().containsKey(datastreamName)))
			return uip;
		
		log.debug("Performing MODS filter operation " + uip.getOperation().name() + " on " + uip.getPID().getPid());
		
		MetadataUIP metadataUIP = (MetadataUIP) uip;
		
		Element newModified = null;
		
		switch (uip.getOperation()) {
			case REPLACE:
				newModified = performReplace(metadataUIP, datastreamName);
				break;
			case ADD:
				newModified = performAdd(metadataUIP, datastreamName);
				break;
			case UPDATE:
				// Doing add for update since the schema does not allow a way to indicate a tag should replace another
				newModified = performAdd(metadataUIP, datastreamName);
				break;
		}

		if (newModified != null) {
			// no validation yet
			//validate(uip, newModified);
			metadataUIP.getModifiedData().put(datastreamName, newModified);
		}
		
		return uip;
	}

}
