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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * Manages and initializes a set of vocabulary helper classes from a mapping of vocabulary types to helper classes.
 * Associates configuration of vocabularies with the original vocabulary content and application levels for specific
 * collections in the repository. Provides helper methods for determining which helpers are applicable and for
 * performing operations over all applicable helpers.
 *
 * @author bbpennel
 * @date Sep 30, 2014
 */
public class VocabularyHelperManager {

    private static final Logger log = LoggerFactory.getLogger(VocabularyHelperManager.class);

    // Map of vocabulary type to helper classes
    private Map<String, Class<?>> helperClassMap;

    // Map of vocabulary type to helper objects
    private Map<String, VocabularyHelper> vocabHelperMap;

    // Map of collection pids to applicable vocabularies
    private Map<String, Set<VocabularyHelper>> pidToHelpers;

    // Map of vocabulary uris to fedora objects those keys apply to
    private Map<String, List<String>> vocabURIToPID;

    // Map of bound pids to application type per vocabulary
    private Map<String, Map<String, Set<String>>> pidToVocabApplication;

    // Configuration info per vocabulary, uri to properties map
    private Map<String, Map<String, String>> vocabInfoMap;

    private Boolean initialized = false;

    private final Namespace[] defaultSelectorNamespaces = new Namespace[] {
            JDOMNamespaceUtil.MODS_V3_NS
    };

    public synchronized void init() {
//        log.debug("Initializing vocabulary helpers");
//        initialized = true;
//
//        // Instantiate helper classes per vocabulary definition
//        instantiateVocabularyHelpers();
//
//        // Establish the associations between containers and vocabularies
//        linkHelpersToPIDs();
//
//        // Load the contents of the vocabularies
//        for (String vocabPID : vocabInfoMap.keySet()) {
//            try {
//                byte[] stream = accessClient.getDatastreamDissemination(new PID(vocabPID),
//                        ContentModelHelper.Datastream.DATA_FILE.getName(), null).getStream();
//
//                if (vocabInfoMap.containsKey(vocabPID)) {
//                    String vocabURI = vocabInfoMap.get(vocabPID).get("vocabURI");
//                    vocabHelperMap.get(vocabURI).setContent(stream);
//                }
//            } catch (Exception e) {
//                log.error("Failed to load vocabulary content for {}", vocabPID, e);
//            }
//        }
//
//        if (log.isDebugEnabled()) {
//            for (Entry<String, Map<String, Set<String>>> pidEntry : pidToVocabApplication.entrySet()) {
//                log.debug("Vocabularies bound to {}", pidEntry.getKey());
//                for (Entry<String, Set<String>> vocabEntry : pidEntry.getValue().entrySet()) {
//                    log.debug("  {}: {}", vocabEntry.getKey(), vocabEntry.getValue());
//                }
//            }
//        }
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
                helperObject.setSelectorNamespaces(defaultSelectorNamespaces);
                helperObject.setSelector(info.get("vocabSelector"));

                vocabHelperMap.put(vocabURI, helperObject);
            } catch (Exception e) {
                log.error("Failed to instantiate vocabulary helper class for {}", info.get("vocabURI"), e);
            }
        }
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
            log.debug("Storing pidtohelper {}", pid);
            for (String vocabUri : entry.getValue().keySet()) {
                if (!vocabHelperMap.containsKey(vocabUri)) {
                    continue;
                }
                helpers.add(vocabHelperMap.get(vocabUri));

                List<String> linkedPIDs = vocabURIToPID.get(vocabUri);
                if (linkedPIDs == null) {
                    linkedPIDs = new ArrayList<>();
                    vocabURIToPID.put(vocabUri, linkedPIDs);
                }
                linkedPIDs.add(pid);
                log.debug("Vocab {} has {}", vocabUri, linkedPIDs);
            }
            log.debug("Helpers so far: {}", helpers);
        }
        log.debug("pidtohelpers: {}", pidToHelpers);
    }

    /**
     * Updates an object's invalid term state in Fedora based on any invalid terms found in the given document
     *
     * @param pid
     * @param docElement
     * @throws FedoraException
     */
    public void updateInvalidTermsRelations(PID pid, Element docElement) throws FedoraException {
//        Set<VocabularyHelper> helpers = getHelpers(pid);
//        if (helpers == null) {
//            return;
//        }
//
//        DatastreamDocument relsDs = managementClient.getXMLDatastreamIfExists(pid, Datastream.RELS_EXT.getName());
//
//        Element descEl = relsDs.getDocument().getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);
//
//        // Remove all existing invalid term predicates
//        boolean termsChanged =
//                descEl.removeChildren(invalidTerm.getPredicate(), invalidTerm.getNamespace());
//
//        for (VocabularyHelper helper : helpers) {
//            Set<String> invalidTerms;
//            try {
//                invalidTerms = helper.getInvalidTermsWithPrefix(docElement);
//
//                if (invalidTerms != null && invalidTerms.size() > 0) {
//                    termsChanged = true;
//
//                    for (String term : invalidTerms) {
//                        Element invTermEl = new Element(invalidTerm.getPredicate(), invalidTerm.getNamespace());
//                        invTermEl.setText(term);
//                        descEl.addContent(invTermEl);
//                    }
//                }
//            } catch (JDOMException e) {
//                log.error("Failed to extract invalid terms from {}", pid.getPid(), e);
//                continue;
//            }
//        }
//
//        // If any terms changed, then update RELS-EXT with optimistic locking
//        if (termsChanged) {
//            do {
//                try {
//                    managementClient.modifyDatastream(pid, RELS_EXT.getName(), "Setting invalid vocabulary terms",
//                            relsDs.getLastModified(), relsDs.getDocument());
//                    return;
//                } catch (OptimisticLockException e) {
//                    log.debug("Unable to update RELS-EXT for {}, retrying", pid);
//                }
//            } while (true);
//        }
    }

    /**
     * Returns a map of invalid terms per vocabulary URI
     *
     * @param pid
     * @param docElement
     * @return
     */
    public Map<String, Set<String>> getInvalidTerms(PID pid, Element docElement) {
        return getInvalidTerms(pid, docElement, false);
    }

    public Map<String, Set<String>> getInvalidTermsWithPrefix(PID pid, Element docElement) {
        return getInvalidTerms(pid, docElement, true);
    }

    private Map<String, Set<String>> getInvalidTerms(PID pid, Element docElement, boolean includePrefix) {
        return null;
//        Set<VocabularyHelper> helpers = getHelpers(pid);
//        if (helpers == null) {
//            return null;
//        }
//
//        Map<String, Set<String>> results = new LinkedHashMap<>();
//
//        for (VocabularyHelper helper : helpers) {
//            if (helper != null) {
//                try {
//                    Set<String> invalidTerms;
//                    if (includePrefix) {
//                        invalidTerms = helper.getInvalidTermsWithPrefix(docElement);
//                    } else {
//                        invalidTerms = helper.getInvalidTerms(docElement);
//                    }
//
//                    if (invalidTerms != null) {
//                        results.put(helper.getVocabularyURI(), invalidTerms);
//                    }
//                } catch (JDOMException e) {
//                    log.error("Failed to get invalid vocabulary terms of {}", pid, e);
//                }
//            }
//        }
//
//        return results;
    }

    /**
     * Get the invalid term predicate for the given vocabulary
     *
     * @param vocabKey
     * @return
     */
    public String getInvalidTermPrefix(String vocabKey) {
        VocabularyHelper helper = vocabHelperMap.get(vocabKey);
        if (helper == null) {
            return null;
        }
        return helper.getInvalidTermPrefix();
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
        return null;
//        synchronized (initialized) {
//            if (!initialized) {
//                init();
//            }
//        }
//
//        PID parentCollectionPID = queryService.fetchParentCollection(pid);
//        if (parentCollectionPID == null) {
//            List<URI> models = queryService.lookupContentModels(pid);
//            if (models.contains(COLLECTION.getURI())) {
//                parentCollectionPID = pid;
//            }
//        }
//
//        // Start helpers from the set of globals assigned to the collections object
//        Set<VocabularyHelper> helpers = new HashSet<>();
//        Set<VocabularyHelper> rootHelpers = pidToHelpers.get(collectionsPID.getURI());
//        if (rootHelpers != null) {
//            helpers.addAll(rootHelpers);
//        }
//
//        if (parentCollectionPID != null) {
//            Set<VocabularyHelper> parentHelpers = pidToHelpers.get(parentCollectionPID.getURI());
//            if (parentHelpers != null) {
//                helpers.addAll(parentHelpers);
//            }
//        }
//
//        if (appLevel != null && helpers != null) {
//            filterHelperSet(parentCollectionPID, helpers, appLevel);
//        }
//
//        if (log.isDebugEnabled()) {
//            log.debug("Helpers found for {} with {}: {}", new Object[] { pid, appLevel, helpers });
//        }
//
//        return helpers;
    }

    private void filterHelperSet(PID pid, Set<VocabularyHelper> helpers, CDRProperty appLevel) {
        if (helpers == null) {
            return;
        }

        Map<String, Set<String>> appLevelMap = pidToVocabApplication.get(RepositoryPaths.getContentRootPid().getURI());
        if (pid != null) {
            Map<String, Set<String>> collectionLevel = pidToVocabApplication.get(pid.getURI());
            if (collectionLevel != null) {
                if (appLevelMap == null) {
                    appLevelMap = collectionLevel;
                } else {
                    appLevelMap.putAll(collectionLevel);
                }
            }
        }

        if (appLevelMap == null) {
            return;
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

    /**
     * Sets the map of helper classes and generates the mapping of vocabulary types to instances of helpers
     *
     * @param helperClasses
     */
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

    /**
     * Returns the authoritative vocabulary forms for selected fields from the given document using all applicable
     * helpers for the specified object. Terms are grouped by vocabulary URI.
     *
     * @param pid
     * @param doc
     * @return
     */
    public Map<String, List<List<String>>> getAuthoritativeForms(PID pid, Document doc) {
        return getAuthoritativeForms(pid, doc.getRootElement());
    }

    public Map<String, List<List<String>>> getAuthoritativeForms(PID pid, Element docElement) {
        return null;
//        Set<VocabularyHelper> helpers = getHelpers(pid);
//        if (helpers == null) {
//            return null;
//        }
//
//        Map<String, List<List<String>>> results = new HashMap<>();
//        for (VocabularyHelper helper : helpers) {
//            try {
//                List<List<String>> terms = helper.getAuthoritativeForms(docElement);
//                if (terms != null && terms.size() > 0) {
//                    results.put(helper.getVocabularyURI(), terms);
//                }
//            } catch (JDOMException e) {
//                log.error("Failed to get authoritative forms for vocabulary {} on object {}",
//                        new Object[] { helper.getVocabularyURI(), pid.getPid(), e });
//            }
//        }
//
//        return results;
    }

}
