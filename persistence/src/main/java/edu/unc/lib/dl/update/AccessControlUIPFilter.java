package edu.unc.lib.dl.update;

import java.util.Iterator;

import org.jdom.Element;

import edu.unc.lib.dl.acl.util.AccessControlTransformationUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.RDFUtil;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Applies updates to the RELS-EXT datastream based on the specialized pseudo-datasteam for access controls.
 * 
 * @author bbpennel
 * 
 */
public class AccessControlUIPFilter extends RELSEXTUIPFilter {
	private final String relsDatastreamName = Datastream.RELS_EXT.getName();
	private final String aclDatastreamName = "ACL";

	@Override
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
		// Only run this filter for metadata update requests
		if (uip == null || !(uip instanceof MetadataUIP))
			return uip;

		MetadataUIP metadataUIP = (MetadataUIP) uip;

		Object incomingObject = uip.getIncomingData().get(aclDatastreamName);
		// Do not apply filter unless the rels-ext ds is being targeted.
		if (incomingObject == null)
			return uip;
		
		metadataUIP.getIncomingData().put(aclDatastreamName, AccessControlTransformationUtil.aclToRDF((Element)incomingObject));
		
		return this.doRelsExtFilter(metadataUIP, relsDatastreamName, aclDatastreamName);
	}

	protected Element performReplace(MetadataUIP uip, String baseDatastream, String incomingDatastream)
			throws UIPException {
		Element incoming = uip.getIncomingData().get(incomingDatastream);
		Element newModified = this.getBaseElement(uip, baseDatastream, incoming);
		if (newModified == null)
			return null;

		// Clear out all the ACL related relations
		Element baseDescription = newModified.getChild("Description", JDOMNamespaceUtil.RDF_NS);
		Iterator<?> relationIt = baseDescription.getChildren().iterator();
		while (relationIt.hasNext()) {
			Element element = (Element) relationIt.next();
			if (JDOMNamespaceUtil.CDR_ROLE_NS.equals(element.getNamespace())
					|| JDOMNamespaceUtil.CDR_ACL_NS.equals(element.getNamespace())) {
				relationIt.remove();
			}
		}

		baseDescription.removeChildren(ContentModelHelper.CDRProperty.allowIndexing.getPredicate(),
				JDOMNamespaceUtil.CDR_NS);
		baseDescription.removeChildren(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
				JDOMNamespaceUtil.CDR_NS);

		return RDFUtil.mergeRDF(newModified, incoming);
	}

}
