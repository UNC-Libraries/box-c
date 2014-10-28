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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.replaceInvalidTerms;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * @author bbpennel
 * @date Sep 30, 2014
 */
public class VocabularyHelperManager {

	private static final Logger log = LoggerFactory.getLogger(VocabularyHelperManager.class);

	// Map of vocabulary keys to helper classes, vocab URI to helper
	private Map<String, Class<?>> helperClassMap;

	// Map of vocabulary keys to helper objects
	private Map<String, VocabularyHelper> vocabHelperMap;

	// Map of collection pids to applicable vocabularies
	private Map<String, Set<VocabularyHelper>> pidToHelpers;

	// Map of vocabulary uris to fedora objects those keys apply to
	private Map<String, List<String>> vocabURIToPID;

	// Map of bound pids to application type per vocabulary
	private Map<String, Map<String, Set<String>>> pidToVocabApplication;

	// Configuration info per vocabulary, uri to properties map
	private Map<String, Map<String, String>> vocabInfoMap;


	private PID collectionsPID;

	@Autowired
	private TripleStoreQueryService queryService;
	@Autowired
	private ManagementClient managementClient;
	@Autowired
	private AccessClient accessClient;

	private boolean initialized = false;

	public void init() {
		// Scan fedora for objects with the vocabulary content model
		vocabInfoMap = queryService.fetchVocabularyInfo();

		pidToVocabApplication = queryService.fetchVocabularyMapping();

		// Instantiate helper classes per vocabulary definition
		instantiateVocabularyHelpers();

		// Establish the associations between containers and vocabularies
		linkHelpersToPIDs();

		// Load the contents of the vocabularies
		for (String vocabPID : vocabInfoMap.keySet()) {
			int tries = -1;
			do {
				try {
					byte[] stream = accessClient.getDatastreamDissemination(new PID(vocabPID),
							ContentModelHelper.Datastream.DATA_FILE.getName(), null).getStream();

					if (vocabInfoMap.containsKey(vocabPID)) {
						String vocabURI = vocabInfoMap.get(vocabPID).get("vocabURI");
						vocabHelperMap.get(vocabURI).setContent(stream);
					}
				} catch (Exception e) {
					log.error("Failed to load vocabulary content for {}", vocabPID, e);
				}
			} while (tries > 0);
		}
	}

	private void instantiateVocabularyHelpers() {
		vocabHelperMap = new HashMap<>();

		for (Map<String, String> info : vocabInfoMap.values()) {
			try {
				String vocabType = info.get("vocabType");
				String vocabURI = info.get("vocabURI");
				if (!helperClassMap.containsKey(vocabType)) {
					log.warn("Vocabulary {} specifies a vocabulary type that does not map to a helper class",
							vocabURI, vocabType);
					continue;
				}

				VocabularyHelper helperObject =
						(VocabularyHelper) helperClassMap.get(vocabType).getConstructor().newInstance();

				helperObject.setVocabularyURI(vocabURI);
				helperObject.setSelector(info.get("vocabularySelector"));

				vocabHelperMap.put(vocabURI, helperObject);
			} catch (Exception e) {
				log.error("Failed to instantiate vocabulary helper class for {}", info.get("vocabURI"), e);
			}
		}
		initialized = true;
	}

	private void linkHelpersToPIDs() {
		if (pidToVocabApplication == null) {
			pidToHelpers = null;
			return;
		}

		pidToHelpers = new HashMap<>();
		vocabURIToPID = new HashMap<>();

		for (Entry<String, Map<String, Set<String>>> entry : this.pidToVocabApplication.entrySet()) {
			String pid = entry.getKey();
			Set<VocabularyHelper> helpers = new HashSet<>();
			pidToHelpers.put(pid, helpers);
			for (String vocabUri : entry.getValue().keySet()) {
				helpers.add(vocabHelperMap.get(vocabUri));

				List<String> linkedPIDs = vocabURIToPID.get(vocabUri);
				if (linkedPIDs == null) {
					linkedPIDs = new ArrayList<>();
					vocabURIToPID.put(vocabUri, linkedPIDs);
				}
				linkedPIDs.add(pid);
			}
		}
	}

	public void updateInvalidTerms(PID pid, Element docElement) throws FedoraException {
		Set<VocabularyHelper> helpers = getHelpers(pid);
		if (helpers == null)
			return;

		for (VocabularyHelper helper : helpers) {
			String relationPredicate = helper.getInvalidTermPredicate();

			List<String> existingTerms = queryService.fetchBySubjectAndPredicate(pid, relationPredicate);

			Set<String> invalidTerms;
			try {
				invalidTerms = helper.getInvalidTerms(docElement);
			} catch (JDOMException e) {
				log.error("Failed to extract invalid terms from {}", pid.getPid(), e);
				continue;
			}

			if (existingTerms != null && invalidTerms.size() == existingTerms.size()
					&& invalidTerms.containsAll(existingTerms)) {
				continue;
			}

			if (existingTerms != null) {
				// Remove any terms which are no longer present
				List<String> removeTerms = new ArrayList<String>(existingTerms);
				removeTerms.removeAll(invalidTerms);

				for (String term : removeTerms) {
					managementClient.purgeLiteralStatement(pid, relationPredicate, term, null);
				}

				// Calculate the set of newly invalid terms which need to be added
				invalidTerms.removeAll(existingTerms);
			}

			if (invalidTerms.size() > 0) {
				for (String term : invalidTerms) {
					managementClient.addLiteralStatement(pid, relationPredicate, term, null);
				}
			}
		}
	}

	public Map<String, Set<String>> getInvalidTerms(PID pid, Element docElement) {

		Set<VocabularyHelper> helpers = getHelpers(pid);
		if (helpers == null)
			return null;

		Map<String, Set<String>> results = new LinkedHashMap<>();

		for (VocabularyHelper helper : helpers) {
			if (helper != null) {
				try {
					Set<String> invalidTerms = helper.getInvalidTerms(docElement);
					if (invalidTerms != null)
						results.put(helper.getVocabularyURI(), invalidTerms);
				} catch (JDOMException e) {
					log.error("Failed to get invalid vocabulary terms of {}", pid, e);
				}
			}
		}

		return results;
	}

	/**
	 * Get the invalid term predicate for the given vocabulary
	 *
	 * @param vocabKey
	 * @return
	 */
	public String getInvalidTermPredicate(String vocabKey) {
		VocabularyHelper helper = vocabHelperMap.get(vocabKey);
		if (helper == null)
			return null;
		return helper.getInvalidTermPredicate();
	}

	/**
	 * Returns the set of vocabulary helpers that apply to the provided pid, as defined on its parent collection
	 *
	 * @param pid
	 * @return
	 */
	public Set<VocabularyHelper> getHelpers(PID pid) {
		return getHelpers(pid, null);
	}

	/**
	 * Returns all helpers for the given pid which have invalid term remapping enabled
	 *
	 * @param pid
	 * @param appLevel
	 * @return
	 */
	public Set<VocabularyHelper> getRemappingHelpers(PID pid) {
		return getHelpers(pid, replaceInvalidTerms);
	}

	private Set<VocabularyHelper> getHelpers(PID pid, CDRProperty appLevel) {
		if (!initialized)
			init();

		PID parentCollectionPID = queryService.fetchParentCollection(pid);
		if (parentCollectionPID == null) {
			List<URI> models = queryService.lookupContentModels(pid);
			if (models.contains(COLLECTION.getURI())) {
				parentCollectionPID = pid;
			}
		}

		// Start helpers from the set of globals assigned to the collections object
		Set<VocabularyHelper> helpers = pidToHelpers.get(collectionsPID.getURI());
		if (parentCollectionPID != null) {
			Set<VocabularyHelper> parentHelpers = pidToHelpers.get(parentCollectionPID.getURI());
			if (parentHelpers != null) {
				if (helpers == null) {
					helpers = parentHelpers;
				} else {
					helpers.addAll(parentHelpers);
				}
			}
		}

		if (appLevel != null && helpers != null)
			filterHelperSet(parentCollectionPID, helpers, appLevel);

		return helpers;
	}

	private void filterHelperSet(PID pid, Set<VocabularyHelper> helpers, CDRProperty appLevel) {
		if (helpers == null)
			return;

		Map<String, Set<String>> appLevelMap = pidToVocabApplication.get(collectionsPID.getURI());
		if (pid != null) {
			if (appLevelMap == null) {
				appLevelMap = pidToVocabApplication.get(pid.getURI());
			} else {
				appLevelMap.putAll(pidToVocabApplication.get(pid.getURI()));
			}
		}

		Iterator<VocabularyHelper> helperIt = helpers.iterator();
		while (helperIt.hasNext()) {
			VocabularyHelper helper = helperIt.next();

			Set<String> appLevels = appLevelMap.get(helper.getVocabularyURI());
			if (!appLevels.contains(appLevel.toString())) {
				helperIt.remove();
			}
		}
	}

	public VocabularyHelper getHelper(String vocabURI) {
		return this.vocabHelperMap.get(vocabURI);
	}

	public void setHelperClasses(Map<String, String> helperClasses) {
		this.helperClassMap = new HashMap<>(helperClasses.size());

		for (Entry<String, String> helperEntry : helperClasses.entrySet()) {
			try {
				this.helperClassMap.put(helperEntry.getKey(), Class.forName(helperEntry.getValue()));
			} catch (ClassNotFoundException e) {
				log.error("Failed to get class for helper {}", helperEntry.getValue(), e);
			}
		}
	}

	public Map<String, List<String>> getAuthoritativeForms(PID pid, Document doc) {
		return getAuthoritativeForms(pid, doc.getRootElement());
	}

	public Map<String, List<String>> getAuthoritativeForms(PID pid, Element docElement) {
		Set<VocabularyHelper> helpers = getHelpers(pid);
		if (helpers == null)
			return null;

		Map<String, List<String>> results = new HashMap<>();
		for (VocabularyHelper helper : helpers) {
			try {
				List<String> terms = helper.getAuthoritativeForms(docElement);
				if (terms != null && terms.size() > 0)
					results.put(helper.getVocabularyURI(), terms);
			} catch (JDOMException e) {
				log.error("Failed to get authoritative forms for vocabulary {} on object {}",
						new Object[] { helper.getVocabularyURI(), pid.getPid(), e });
			}
		}

		return results;
	}

	public void setCollectionsPID(PID collectionsPID) {
		this.collectionsPID = collectionsPID;
	}
}
