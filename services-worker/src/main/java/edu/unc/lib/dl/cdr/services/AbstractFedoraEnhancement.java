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

import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.DatastreamDocument;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public abstract class AbstractFedoraEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractFedoraEnhancement.class);
	
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
			throws FedoraException{
		setExclusiveTriple(pid, predicate, namespace, exclusivePID.toString(), false, null, foxml);
	}
	
	protected void setExclusiveTripleValue(PID pid, String predicate, Namespace namespace, String newExclusiveValue,
			String datatype, Document foxml) throws FedoraException {
		setExclusiveTriple(pid, predicate, namespace, newExclusiveValue, true, datatype, foxml);
	}
	
	protected void setExclusiveTriple(PID pid, String predicate, Namespace namespace, String value,
			boolean isLiteral, String datatype, Document foxml)
			throws FedoraException {
		DatastreamDocument dsDoc = service.getManagementClient().getRELSEXTWithRetries(pid);
		
		do {
			try {
				Document doc = dsDoc.getDocument();
				Element descEl = doc.getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);
				
				descEl.removeChildren(predicate, namespace);
				
				Element relEl = new Element(predicate, namespace);
				if (isLiteral) {
					if (datatype != null) {
						relEl.setAttribute("datatype", datatype, JDOMNamespaceUtil.RDF_NS);
					}
					relEl.setText(value);
				} else {
					relEl.setAttribute("resource", value, JDOMNamespaceUtil.RDF_NS);
				}
				
				descEl.addContent(relEl);
				
				service.getManagementClient().modifyDatastream(pid, RELS_EXT.getName(),
						"Setting exclusive relation", dsDoc.getLastModified(), dsDoc.getDocument());
				return;
			} catch (OptimisticLockException e) {
				LOG.debug("Unable to update RELS-EXT for {}, retrying", pid, e);
			}
		} while (true);
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
