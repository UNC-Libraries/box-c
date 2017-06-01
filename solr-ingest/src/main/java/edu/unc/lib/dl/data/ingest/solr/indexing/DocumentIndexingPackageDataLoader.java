/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.exception.OrphanedObjectException;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.Repository;
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

/**
 * Loads data to populate fields in a DocumentIndexingPackage
 * 
 * @author bbpennel
 * @date Jun 22, 2015
 */
public class DocumentIndexingPackageDataLoader {
	private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackageDataLoader.class);
	
	private Repository repository;
	
	private ManagementClient managementClient ;
	private AccessClient accessClient;
	private TripleStoreQueryService tsqs;
	private AccessControlService accessControlService;
	private DocumentIndexingPackageFactory factory;
	
	private int maxRetries = 2;
	private long retryDelay = 1000L;
	
	private LoadingCache<String, List<Entry<String, String>>> objCache;
	private long cacheTimeToLive;
	private long cacheMaxSize;
	
	public void init() {
		objCache = CacheBuilder.newBuilder()
				.maximumSize(cacheMaxSize)
				.expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
				.build(new ObjectCacheLoader());
	}
	
	public Element loadMods(DocumentIndexingPackage dip) throws IndexingException {
		ContentObject contentObj = repository.getContentObject(dip.getPid());
		BinaryObject modsBinary = contentObj.getMODS();
		
		try (InputStream modsStream = modsBinary.getBinaryStream()) {
			Document dsDoc = new SAXBuilder().build(modsStream);
			return dsDoc.detachRootElement();
		} catch (JDOMException | IOException e) {
			throw new ServiceException("Failed to parse MODS stream for object " + dip.getPid(), e);
		}
	}
	
	private List<Entry<String, String>> getObjectProperties(PID pid) {
		String pidString = pid.getRepositoryPath();
		return objCache.getUnchecked(pidString);
	}

	public long getCacheTimeToLive() {
		return cacheTimeToLive;
	}

	public void setCacheTimeToLive(long cacheTimeToLive) {
		this.cacheTimeToLive = cacheTimeToLive;
	}

	public long getCacheMaxSize() {
		return cacheMaxSize;
	}

	public void setCacheMaxSize(long cacheMaxSize) {
		this.cacheMaxSize = cacheMaxSize;
	}
	
	/**
	 * Loader for cache of information about individual objects. Retrieves
	 * properties from a SPARQL endpoint which are directly present on
	 * objects
	 * 
	 * @author bbpennel
	 *
	 */
	private class ObjectCacheLoader extends CacheLoader<String, List<Entry<String, String>>> {
		
		// query string for returning all of an object's triples
		private static final String OBJ_QUERY = "SELECT ?pred ?obj"
				+ " WHERE { <%1$s> ?pred ?obj . }";

		public List<Entry<String, String>> load(String key) {
			return null;

//			String query = String.format(OBJ_QUERY, key);
//
//			try (QueryExecution qExecution = queryService.executeQuery(query)) {
//				ResultSet resultSet = qExecution.execSelect();
//				List<Entry<String, String>> valueResults = new ArrayList<>();
//
//				// Read all results into a list of predicate object pairs
//				for (; resultSet.hasNext() ;) {
//					QuerySolution soln = resultSet.nextSolution();
//					Resource predicateRes = soln.getResource("pred");
//					RDFNode valueNode = soln.get("obj");
//
//					if (predicateRes != null && valueNode != null) {
//						String predicateString = predicateRes.getURI();
//						String valueString;
//						if (valueNode.isLiteral()) {
//							valueString = valueNode.asLiteral().getLexicalForm();
//						} else {
//							valueString = valueNode.asResource().getURI();
//						}
//
//						valueResults.add(new SimpleEntry<String, String>
//								(predicateString, valueString));
//					}
//				}
//
//				return valueResults;
//			}
		}
	}

	@Deprecated
	public Document loadFOXML(DocumentIndexingPackage dip) throws IndexingException {
		PID pid = dip.getPid();
		try {
			log.debug("Retrieving FOXML for {}", pid.getPidAsString());

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
						log.warn("Failed to retrieve FOXML for " + pid.getPidAsString() + ", retrying.", e);
					} else {
						throw new IndexingException("Failed to retrieve FOXML for " + pid.getPidAsString() + " after " + maxRetries
								+ " tries.", e);
					}
				}
			} while (--tries > 0);

			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPidAsString());
		} catch (FedoraException e) {
			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPidAsString(), e);
		} catch (InterruptedException e) {
			throw new IndexingException("Interrupted while waiting to retry FOXML retrieval for " + pid.getPidAsString(), e);
		}
	}
	@Deprecated
	public ObjectAccessControlsBean loadAccessControlBean(DocumentIndexingPackage dip) throws IndexingException {
		if (!dip.hasParentDocument() || !dip.getParentDocument().hasAclBean()) {
			// No parent object, ask fedora for access control
			return accessControlService.getObjectAccessControls(dip.getPid());
		}
		return new ObjectAccessControlsBeanImpl(dip.getParentDocument().getAclBean(), dip.getPid(), dip.getTriples());
	}
	@Deprecated
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
	@Deprecated
	public Map<String, List<String>> loadTriples(DocumentIndexingPackage dip) throws IndexingException {
		return tsqs.fetchAllTriples(dip.getPid());
	}
	@Deprecated
	public DocumentIndexingPackage loadParentDip(DocumentIndexingPackage dip) throws IndexingException {
		PID parentPid = dip.getParentPid();
		return factory.createDip(parentPid);
	}
	@Deprecated
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
				log.debug("Retrieving parent pid for " + dip.getPid().getPidAsString());
				parentPID = tsqs.fetchByPredicateAndLiteral(ContentModelHelper.Relationship.contains.toString(), dip.getPid()).get(0);
			} catch (IndexOutOfBoundsException e) {
				throw new OrphanedObjectException("Could not retrieve parent pid for " + dip.getPid().getPidAsString());
			}
		}
		
		return parentPID;
	}
	@Deprecated
	public DocumentIndexingPackage loadDefaultWebObject(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = dip.getTriples();
		
		List<String> defaultWebObject = triples.get(CDRProperty.defaultWebObject.getURI().toString());
		
		if (defaultWebObject != null && defaultWebObject.size() > 0) {
			return factory.createDip(new PID(defaultWebObject.get(0)));
		}
		
		return null;
	}
	@Deprecated
	public String loadDefaultWebData(DocumentIndexingPackage dip) throws IndexingException {
		String defaultWebData = dip.getFirstTriple(CDRProperty.defaultWebData.toString());
		// If this object does not have a defaultWebData but its defaultWebObject does, then use that instead.
		if (defaultWebData == null && dip.getDefaultWebObject() != null) {
			defaultWebData = dip.getDefaultWebObject().getFirstTriple(CDRProperty.defaultWebData.toString());
		}
		
		return defaultWebData;
	}
	@Deprecated
	public Element loadMDDescriptive(DocumentIndexingPackage dip) throws IndexingException {
		return loadDatastream(dip, Datastream.MD_DESCRIPTIVE, true);
	}
	@Deprecated
	public Element loadMDContents(DocumentIndexingPackage dip) throws IndexingException {
		return loadDatastream(dip, Datastream.MD_CONTENTS, true);
	}
	@Deprecated
	private Element getDatastream(DocumentIndexingPackage dip, Datastream ds) throws IndexingException {
		Element dsEl = null;
		if (dip.hasFoxml()) {
			dsEl = FOXMLJDOMUtil.getDatastreamContent(ds, dip.getFoxml());
		}
		return dsEl;
	}
	@Deprecated
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
	@Deprecated
	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
	@Deprecated
	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}
	@Deprecated
	public void setTsqs(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}
	@Deprecated
	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}
	@Deprecated
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}
	@Deprecated
	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}
	@Deprecated
	public void setFactory(DocumentIndexingPackageFactory factory) {
		this.factory = factory;
	}
	
}
