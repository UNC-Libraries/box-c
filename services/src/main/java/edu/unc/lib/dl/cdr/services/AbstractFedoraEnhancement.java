package edu.unc.lib.dl.cdr.services;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public abstract class AbstractFedoraEnhancement extends Enhancement<Element> {
	protected AbstractFedoraEnhancementService service;
	
	protected AbstractFedoraEnhancement(AbstractFedoraEnhancementService service, PID pid) {
		super(pid);
		this.service = service;
	}
	
	protected String getDSLocation(String dsid, String vid, Document foxml) {
		Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, dsid);
		for (Object o : dsEl.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)) {
			if (o instanceof Element) {
				Element dsvEl = (Element) o;
				if (vid.equals(dsvEl.getAttributeValue("ID"))) {
					return dsvEl.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS)
							.getAttributeValue("REF");
				}
			}
		}
		
		return null;
	}
	
	protected void setExclusiveTripleRelation(PID pid, String predicate, PID exclusivePID)
			throws FedoraException {
		List<String> rel = service.getTripleStoreQueryService().fetchAllTriples(pid).get(predicate);
		
		if (rel != null) {
			String valueString = exclusivePID.toString();
			if (rel.contains(valueString)) {
				rel.remove(valueString);
			} else {
				// add missing rel
				service.getManagementClient().addObjectRelationship(pid, predicate, exclusivePID);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				service.getManagementClient().purgeObjectRelationship(pid, predicate, new PID(oldValue));
			}
		} else {
			// add missing rel
			service.getManagementClient().addObjectRelationship(pid, predicate, exclusivePID);
		}
	}

	protected void setExclusiveTripleValue(PID pid, String predicate, String newExclusiveValue, String datatype)
			throws FedoraException {
		List<String> rel = service.getTripleStoreQueryService().fetchAllTriples(pid).get(predicate);
		if (rel != null) {
			if (rel.contains(newExclusiveValue)) {
				rel.remove(newExclusiveValue);
			} else {
				// add missing rel
				service.getManagementClient().addLiteralStatement(pid, predicate, newExclusiveValue, datatype);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				service.getManagementClient().purgeLiteralStatement(pid, predicate, oldValue, datatype);
			}
		} else {
			// add missing rel
			service.getManagementClient().addLiteralStatement(pid, predicate, newExclusiveValue, datatype);
		}
	}
}
