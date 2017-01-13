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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.exception.OrphanedObjectException;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.NamespaceConstants;

/**
 * Loads data to populate fields in a DocumentIndexingPackage
 * 
 * @author bbpennel
 * @date Jun 22, 2015
 */
public class DocumentIndexingPackageDataLoader {
	private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackageDataLoader.class);
	
	private static final String OBJECT_STATE_RELATION = ContentModelHelper.FedoraProperty.state.toString();
	
	private ManagementClient managementClient ;
	private AccessClient accessClient;
	private TripleStoreQueryService tsqs;
	private AccessControlService accessControlService;
	private DocumentIndexingPackageFactory factory;
	
	private int maxRetries = 2;
	private long retryDelay = 1000L;

	
	public Document loadFOXML(DocumentIndexingPackage dip) throws IndexingException {
		PID pid = dip.getPid();
		try {
			log.debug("Retrieving FOXML for {}", pid.getPid());

			Document foxml = null;
			int tries = maxRetries;
			do {

				if (tries < maxRetries) {
					Thread.sleep(retryDelay);
					log.debug("Retrieving FOXML for DIP, tries remaining: {}", tries);
				}

				try {
					foxml = managementClient.getObjectXML(pid);
					if (foxml != null) {
						return foxml;
					}

				} catch (ServiceException | NotFoundException e) {
					// If there are retries left, retry on service exception
					if (tries > 1) {
						log.warn("Failed to retrieve FOXML for " + pid.getPid() + ", retrying.", e);
					} else {
						throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid() + " after " + maxRetries
								+ " tries.", e);
					}
				}
			} while (--tries > 0);

			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid());
		} catch (FedoraException e) {
			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid(), e);
		} catch (InterruptedException e) {
			throw new IndexingException("Interrupted while waiting to retry FOXML retrieval for " + pid.getPid(), e);
		}
	}
	
	public ObjectAccessControlsBean loadAccessControlBean(DocumentIndexingPackage dip) throws IndexingException {
		if (!dip.hasParentDocument() || !dip.getParentDocument().hasAclBean()) {
			// No parent object, ask fedora for access control
			return accessControlService.getObjectAccessControls(dip.getPid());
		}
		return new ObjectAccessControlsBeanImpl(dip.getParentDocument().getAclBean(), dip.getPid(), dip.getTriples());
	}
	
	public List<PID> loadChildren(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = dip.getTriples();
		
		List<String> childrenRelations = triples.get(Relationship.contains.toString());
		
		if (childrenRelations == null) {
			return Collections.<PID>emptyList();
		}
		List<PID> children = new ArrayList<PID>(childrenRelations.size());
		
		for (String childRelation : childrenRelations) {
			children.add(new PID(childRelation));
		}

		return children;
	}
	
	public Map<String, List<String>> loadTriples(DocumentIndexingPackage dip) throws IndexingException {
		if (dip.hasFoxml()) {
			return this.extractTriples(dip);
		}
		return tsqs.fetchAllTriples(dip.getPid());
	}
	
	private Map<String, List<String>> extractTriples(DocumentIndexingPackage dip) throws IndexingException {
		PID pid = dip.getPid();
		
		Element objectProperties = FOXMLJDOMUtil.getObjectProperties(dip.getFoxml());
		Element relsExt = getDatastream(dip, Datastream.RELS_EXT);
		Map<String, Element> datastreams = FOXMLJDOMUtil.getMostRecentDatastreamMap(dip.getFoxml());

		Map<String, List<String>> triples = new HashMap<String, List<String>>();
		if (relsExt != null) {
			List<?> tripleEls = relsExt.getChild("Description", JDOMNamespaceUtil.RDF_NS).getChildren();
			for (Object tripleObject : tripleEls) {
				Element tripleEl = (Element) tripleObject;
				String predicate = tripleEl.getNamespaceURI() + tripleEl.getName();
				String value = tripleEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS);
				if (value == null)
					value = tripleEl.getText();
				List<String> predicateTriples = triples.get(predicate);
				if (predicateTriples == null) {
					predicateTriples = new ArrayList<String>();
					triples.put(predicate, predicateTriples);
				}
				predicateTriples.add(value);
			}
		}

		if (objectProperties != null) {
			List<?> tripleEls = objectProperties.getChildren();
			for (Object tripleObject : tripleEls) {
				Element tripleEl = (Element) tripleObject;
				String predicate = tripleEl.getAttributeValue("NAME");
				String value = tripleEl.getAttributeValue("VALUE");
				// Fedora prefixes the state value into a URI in the triple store, so add in prefix
				if (OBJECT_STATE_RELATION.equals(predicate))
					value = NamespaceConstants.FEDORA_MODEL_URI + value;
				List<String> predicateTriples = triples.get(predicate);
				if (predicateTriples == null) {
					predicateTriples = new ArrayList<String>();
					triples.put(predicate, predicateTriples);
				}
				predicateTriples.add(value);
			}
		}

		if (datastreams.size() > 0) {
			List<String> predicateTriples = new ArrayList<String>();
			triples.put(ContentModelHelper.FedoraProperty.disseminates.toString(), predicateTriples);
			for (String datastream : datastreams.keySet()) {
				predicateTriples.add(pid.getURI() + "/" + datastream);
			}
		}
		
		return triples;
	}

	public DocumentIndexingPackage loadParentDip(DocumentIndexingPackage dip) throws IndexingException {
		PID parentPid = dip.getParentPid();
		return factory.createDip(parentPid);
	}

	public PID loadParentPid(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		PID parentPID = null;
		// Try to get the parent pid from the items ancestors if available.
		if (idb.getAncestorPath() != null && idb.getAncestorPath().size() > 0) {
			String ancestor = idb.getAncestorPath().get(idb.getAncestorPath().size() - 1);
			int index = ancestor.indexOf(',');
			ancestor = ancestor.substring(index + 1);
			index = ancestor.indexOf(',');
			ancestor = ancestor.substring(0, index);
			parentPID = new PID(ancestor);
		} else {
			try {
				log.debug("Retrieving parent pid for " + dip.getPid().getPid());
				parentPID = tsqs.fetchByPredicateAndLiteral(ContentModelHelper.Relationship.contains.toString(), dip.getPid()).get(0);
			} catch (IndexOutOfBoundsException e) {
				throw new OrphanedObjectException("Could not retrieve parent pid for " + dip.getPid().getPid());
			}
		}
		
		return parentPID;
	}

	public DocumentIndexingPackage loadDefaultWebObject(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = dip.getTriples();
		
		List<String> defaultWebObject = triples.get(CDRProperty.defaultWebObject.getURI().toString());
		
		if (defaultWebObject != null && defaultWebObject.size() > 0) {
			return factory.createDip(new PID(defaultWebObject.get(0)));
		}
		
		return null;
	}
	
	public String loadDefaultWebData(DocumentIndexingPackage dip) throws IndexingException {
		String defaultWebData = dip.getFirstTriple(CDRProperty.defaultWebData.toString());
		// If this object does not have a defaultWebData but its defaultWebObject does, then use that instead.
		if (defaultWebData == null && dip.getDefaultWebObject() != null) {
			defaultWebData = dip.getDefaultWebObject().getFirstTriple(CDRProperty.defaultWebData.toString());
		}
		
		return defaultWebData;
	}
	
	public Element loadMDDescriptive(DocumentIndexingPackage dip) throws IndexingException {
		return loadDatastream(dip, Datastream.MD_DESCRIPTIVE, true);
	}
	
	public Element loadMDContents(DocumentIndexingPackage dip) throws IndexingException {
		return loadDatastream(dip, Datastream.MD_CONTENTS, true);
	}
	
	private Element getDatastream(DocumentIndexingPackage dip, Datastream ds) throws IndexingException {
		Element dsEl = null;
		if (dip.hasFoxml()) {
			dsEl = FOXMLJDOMUtil.getDatastreamContent(ds, dip.getFoxml());
		}
		return dsEl;
	}
	
	private Element loadDatastream(DocumentIndexingPackage dip, Datastream ds, boolean checkFoxml) throws IndexingException {
		PID pid = dip.getPid();
		String datastreamName = ds.getName();
		
		log.debug("Attempting to get datastream " + datastreamName + " for object " + pid);
		
		if (checkFoxml) {
			Element dsEl = getDatastream(dip, ds);
			if (dsEl != null) {
				return dsEl;
			}
		}
		
		try {
			while (true) {
		
				edu.unc.lib.dl.fedora.types.Datastream datastream = managementClient.getDatastream(pid, datastreamName);
			
				if (datastream == null) {
					return null;
				}
			
				try {
					MIMETypedStream mts = accessClient.getDatastreamDissemination(pid, datastreamName, null);
				
					try (ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream())) {
						Document dsDoc = new SAXBuilder().build(bais);
						return dsDoc.detachRootElement();
					} catch (JDOMException | IOException e) {
						throw new ServiceException("Failed to parse datastream " + datastreamName + " for object " + pid, e);
					}
					
				} catch (NotFoundException e) {
					log.debug("No dissemination version for datastream {} on object {} found, retrying",
							datastreamName, pid);
				}
				
			}
			
		} catch (NotFoundException e) {
			return null;
		} catch (FedoraException e) {
			throw new IndexingException("Failed to get datastream " + datastreamName + " for object " + pid, e);
		}
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setTsqs(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}

	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}

	public void setFactory(DocumentIndexingPackageFactory factory) {
		this.factory = factory;
	}
	
}
