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

import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public abstract class AbstractFedoraEnhancement extends Enhancement<Element> {
	
	protected AbstractFedoraEnhancementService service;
	protected EnhancementMessage message;
	protected ManagementClient client;
	
	protected AbstractFedoraEnhancement(AbstractFedoraEnhancementService service, PID pid) {
		super(pid);
		this.service = service;
		this.message = null;
		this.client = service.getManagementClient();
	}
	
	protected AbstractFedoraEnhancement(AbstractFedoraEnhancementService service, EnhancementMessage message) {
		super(message.getPid());
		this.message = message;
		this.service = service;
		this.client = service.getManagementClient();
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
