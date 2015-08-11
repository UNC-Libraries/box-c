/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

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
	
	protected void setExclusiveTripleRelation(PID pid, String predicate, Namespace namespace, PID exclusivePID, Document foxml)
			throws FedoraException {
		List<String> rel = FOXMLJDOMUtil.getRelationValues(predicate, namespace, FOXMLJDOMUtil.getRelsExt(foxml));
		String predicateUri = namespace.getURI() + predicate;
		if (rel != null) {
			String valueString = exclusivePID.toString();
			if (rel.contains(valueString)) {
				rel.remove(valueString);
			} else {
				// add missing rel
				service.getManagementClient().addObjectRelationship(pid, predicateUri, exclusivePID);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				service.getManagementClient().purgeObjectRelationship(pid, predicateUri, new PID(oldValue));
			}
		} else {
			// add missing rel
			service.getManagementClient().addObjectRelationship(pid, predicateUri, exclusivePID);
		}
	}
	
	protected void setExclusiveTripleValue(PID pid, String predicate, Namespace namespace, String newExclusiveValue, String datatype, Document foxml)
			throws FedoraException {
		List<String> rel = FOXMLJDOMUtil.getRelationValues(predicate, namespace, FOXMLJDOMUtil.getRelsExt(foxml));
		String predicateUri = namespace.getURI() + predicate;
		if (rel != null) {
			if (rel.contains(newExclusiveValue)) {
				rel.remove(newExclusiveValue);
			} else {
				// add missing rel
				service.getManagementClient().addLiteralStatement(pid, predicateUri, newExclusiveValue, datatype);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				service.getManagementClient().purgeLiteralStatement(pid, predicateUri, oldValue, datatype);
			}
		} else {
			// add missing rel
			service.getManagementClient().addLiteralStatement(pid, predicateUri, newExclusiveValue, datatype);
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
