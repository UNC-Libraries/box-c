package edu.unc.lib.dl.cdr.services;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public abstract class AbstractFedoraEnhancement extends Enhancement<Element> {
	protected AbstractFedoraEnhancementService service;
	protected EnhancementMessage message;
	
	protected AbstractFedoraEnhancement(AbstractFedoraEnhancementService service, PID pid) {
		super(pid);
		this.service = service;
		this.message = null;
	}
	
	protected AbstractFedoraEnhancement(AbstractFedoraEnhancementService service, EnhancementMessage message) {
		super(message.getPid());
		this.message = message;
		this.service = service;
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
	
	protected Document retrieveFoxml() throws FedoraException {
		if (message != null) {
			if (message.getFoxml() == null) {
				Document foxml = service.getManagementClient().getObjectXML(pid);
				message.setFoxml(foxml);
			}
			return message.getFoxml();
		}
		
		return service.getManagementClient().getObjectXML(pid);
	}
	
	protected List<String> getSourceData() throws FedoraException {
		return getSourceData(this.retrieveFoxml());
	}
	
	protected List<String> getSourceData(Document foxml) throws FedoraException {
		Element relsExt = FOXMLJDOMUtil.getRelsExt(foxml);
		return FOXMLJDOMUtil.getRelationValues(ContentModelHelper.CDRProperty.sourceData.getPredicate(), JDOMNamespaceUtil.CDR_NS, relsExt);
	}
}
