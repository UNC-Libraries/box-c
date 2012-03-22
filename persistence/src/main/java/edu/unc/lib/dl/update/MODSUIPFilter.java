package edu.unc.lib.dl.update;

import org.jdom.Element;

import edu.unc.lib.dl.util.ContentModelHelper;

public class MODSUIPFilter implements UIPUpdateFilter {

	@Override
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
		//Only run this filter for metadata update requests
		if (!(uip instanceof MetadataUIP))
			return uip;
		MetadataUIP metadataUIP = (MetadataUIP)uip;
		
		switch (uip.getOperation()){
			case REPLACE:
				performReplace(metadataUIP);
				break;
		}
		
		return uip;
	}

	private void performReplace(MetadataUIP uip){
		uip.getModifiedData().put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), 
				(Element)uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
	}
	
}
