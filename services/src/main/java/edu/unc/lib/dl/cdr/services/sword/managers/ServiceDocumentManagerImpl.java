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
package edu.unc.lib.dl.cdr.services.sword.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;

/**
 * Generates service document from all containers which are the immediate children of the starting path.
 * 
 * @author bbpennel
 */
public class ServiceDocumentManagerImpl extends AbstractFedoraManager implements ServiceDocumentManager {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceDocumentManagerImpl.class);
	
	private List<String> acceptablePackaging;
	
	public ServiceDocument getServiceDocument(String sdUri, AuthCredentials auth, SwordConfiguration config)
			throws SwordError, SwordServerException, SwordAuthException {
		
		ServiceDocument sd = new ServiceDocument();
		SwordWorkspace workspace = new SwordWorkspace();
		
		String pid = null;
		if (sdUri != null){
			try {
				pid = sdUri.substring(sdUri.lastIndexOf("/") + 1);
			} catch (IndexOutOfBoundsException e){
				//Ignore
			}
		}
		if (pid == null || "".equals(pid)){
			pid = collectionsPidObject.getPid();
		}
		
		List<SwordCollection> collections;
		try {
			collections = this.getImmediateContainerChildren(pid);
			for (SwordCollection collection: collections){
				workspace.addCollection(collection);
			}
			sd.addWorkspace(workspace);
			
			return sd;
		} catch (Exception e) {
			LOG.error("An exception occurred while generating the service document for " + pid, e);
		}

		return null;
	}

	protected List<SwordCollection> getImmediateContainerChildren(String pid) throws IOException {
		String query = this.readFileAsString("immediateContainerChildren.sparql");
		PID pidObject = new PID(pid);
		query = String.format(query, tripleStoreQueryService.getResourceIndexModelUri(), pidObject.getURI());
		List<SwordCollection> result = new ArrayList<SwordCollection>();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<Map> bindings = (List<Map>) ((Map) tripleStoreQueryService.sendSPARQL(query).get("results")).get("bindings");
		for (Map<?, ?> binding : bindings) {
			SwordCollection collection = new SwordCollection();
			PID containerPID = new PID((String) ((Map<?, ?>) binding.get("pid")).get("value"));
			String slug = (String) ((Map<?, ?>) binding.get("slug")).get("value");

			collection.setHref(swordPath + "collection/" + containerPID.getPid());
			collection.setTitle(slug);
			collection.setAccept("*/*");
			collection.addAcceptPackaging(PackagingType.METS_DSPACE_SIP.toString());
			collection.addAcceptPackaging(PackagingType.SIMPLE_ZIP.toString());
			collection.addAcceptPackaging(PackagingType.METS_CDR.toString());
			//
			IRI iri = new IRI(swordPath + "servicedocument/" + containerPID.getPid());
			collection.addSubService(iri);
			result.add(collection);
		}
		return result;
	}
}
