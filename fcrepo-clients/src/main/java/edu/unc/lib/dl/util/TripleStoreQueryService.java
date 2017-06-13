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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Model;

/**
 * 
 * @author count0
 *
 */
public interface TripleStoreQueryService {

    public class PathInfo {
        private String label;
        private String path;
        private PID pid;
        private String slug;

        public String getLabel() {
            return label;
        }

        public String getPath() {
            return path;
        }

        public PID getPid() {
            return pid;
        }

        public String getSlug() {
            return slug;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setPid(PID pid) {
            this.pid = pid;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }

    /**
     * Retrieves a list of all the objects that are directly or indirectly
     * contained by the specified object according to the Fedora relationship
     * ontology. No particular order is guarranteed.
     *
     * @param key
     *            the object
     * @return the list of contained objects
     */
    public abstract List<PID> fetchAllContents(PID key);

    /**
     * Retrieves a list of PID for the children of the supplied container PID.
     *
     * @param parent
     *           the PID of the parent Container
     * @return a list of PIDs
     */
    public List<PID> fetchChildren(PID key);

    /**
     * Retrieves a list of slugs for the children of the supplied container PID.
     *
     * @param parent
     *           the PID of the parent Container
     * @return a list of slug strings
     */
    public Map<String, PID> fetchChildSlugs(PID parent);

    /**
     * Retrieves a list of slugs for the children of the supplied container PID.
     *
     * @param parent
     *           the PID of the parent Container
     * @return a list of slug strings
     */
    public List<PathInfo> fetchChildPathInfo(PID parent);

    /**
     * Retrieves a list of all the containers that are directly contained by the specified container according to the
     * Fedora relationship ontology. No particular order is guarranteed.
     *
     * @param key
     *           the parent container
     * @return the list of contained containers
     */
    public abstract List<PID> fetchChildContainers(PID key);

    /**
     * Retrieves a list of object IDs whose PID URIs are the subject of the specified predicate and literal.
     *
     * @param predicateURI
     * @param literal
     * @return a list of object IDs the include repository paths and PIDs
     */
    public abstract List<PID> fetchByPredicateAndLiteral(String predicateURI, String literal);

    public abstract List<PID> fetchByPredicateAndLiteral(String predicateURI, PID pidLiteral);

    /**
     * Retrieves an object ID based on repository path, i.e. container-slug path.
     *
     * @param path
     *           a repository-unique path string
     * @return the PID
     */
    public abstract PID fetchByRepositoryPath(String path);

    /**
     * No longer supporting a separate GUID other than PID. The UUID-based PID is the new GUID. Retrieves an object ID
     * based on GUID metadata.
     *
     * @param guid
     *           a globally unique ID string
     * @return the digital object ID
     */
    // public abstract PID fetchByUID(String guid);

    /**
     * Retrieves the parent object or container that holds the specified object.
     *
     * @param key
     *           the contained object
     * @return the parent container
     */
    public abstract PID fetchContainer(PID key);

    /**
     * Retrieves the parent object or container that holds the specified object.
     *
     * @param child
     * @param membershipRelation
     * @return
     */
    public abstract PID fetchContainer(PID child, Resource membershipRelation);

    /**
     * Returns whether the object's path can be traced back to the Collections object
     * @param key
     * @return
     */
    public boolean isOrphaned(PID key);

    /**
     * Retrieves the nearest collection that directly or indirectly contains the specified object.
     *
     * @param key
     *           the contained object
     * @return the parent collection
     */
    public abstract PID fetchParentCollection(PID key);

    /**
     * Retrieves the nearest container with the specified model containing the given object
     *
     * @param key
     * @param model
     * @return
     */
    public abstract PID fetchParentByModel(PID key, Model model);

    /**
     * Fetches the PIDs of all objects that reference this one in RELS-EXT, as in they are the subjects of statements
     * where this pid is the object.
     *
     * @param pid
     * @return
     */
    public abstract List<PID> fetchObjectReferences(PID pid);

    /**
     * Returns the URI of the resource index used by Fedora. This is helpful in constructing RI queries, using the
     * following ITQL syntax: <code>select $value from <the resource index URI> where ... </code>
     *
     * @return the URI of the resource index
     */
    public String getResourceIndexModelUri();

    /**
     * Tests the object to see if it is a container.
     *
     * @return true if object is a container, false if not
     */
    public boolean isContainer(PID key);

    /**
     * Retrieves the name of the content model type set on the specified object.
     *
     * @param key
     *           the object ID to lookup
     * @return the identifier of the content model object type
     */
    public abstract List<URI> lookupContentModels(PID key);

    /**
     * Retrieves the path of the Item within the repository, from the first step above the repository root through the
     * Item itself.
     *
     * @param key
     *           the object ID to lookup
     * @return the repository path string, starting from the first object after the REPOSITORY object. Returns "/" if
     *         the object is an orphan.  Path is formatted as a file path, concatenated with /'s.
     */
    public abstract String lookupRepositoryPath(PID key);

    /**
     * Generates a list containing PathInfo objects for each hierarchical step in the path beginning with the repository
     * root leading up to and including PID pid.
     *
     * @param key
     *           the pid for the object of interest
     * @return an ordered list of PathInfo objects starting from the REPOSITORY object or an empty list if the object is
     *         an orphan.
     */
    public abstract List<PathInfo> lookupRepositoryPathInfo(PID key);

    /**
     * Gathers PID of each step in the repository container structure and whether each inherits.
     *
     * @param pid
     *           the pid for the object of interest
     * @return a map of child pids strings to their parent bonds.
     */
    public abstract Map<PID, ParentBond> lookupRepositoryAncestorInheritance(PID pid);

    /**
     * Gathers each parent container in turn along with whether or not roles are inherited.
     *
     * @param pid
     *           the pid for the object of interest
     * @return an ordered list of parent bond objects starting the immediate parent.
     */
    public abstract List<PID> lookupRepositoryAncestorPids(PID pid);

    /**
     * Returns the set of all property-subject pairs on this object.
     *
     * @param pid
     *           the pid for the object of interest
     * @return a map keyed by property with values including the set of subjects with that property.
     */
    public abstract Map<String, List<String>> fetchAllTriples(PID pid);

    /**
     * Returns the set of a specific permission property-subject pairs on this object.
     *
     * @param pid
     *           the pid for the object of interest
     * @param permission
     *           The specific permission of interest
     * @return a map keyed by permission property with values including the set of subjects with that permission. Will
     *         also return no inherit entries.
     */
    public abstract Map<String, List<String>> lookupSinglePermission(PID pid, String permission);

    /**
     * Returns all the embargoes still active by pid.
     *
     * @return a map keyed by PID with values of embargo date.
     */
    public abstract Map<PID, String> fetchEmbargoes();

    /**
     * Builds a list of all the containers above this one, starting with the REPOSITORY container and ending with the
     * parent.
     *
     * @param pid
     *           The PID of the contained object in question
     * @return
     */
    public abstract List<PID> lookupAllContainersAbove(PID pid);

    /**
     * Is this datastream cdr:sourceData?
     *
     * @param pid
     *           the pid for the object of interest
     * @param dsid
     *           the datastream ID
     * @return a simple boolean answer
     */
    public abstract boolean isSourceData(PID pid, String datastreamID);

    public abstract boolean allowIndexing(PID pid);

    /**
     * Gets datastreams that are cdr:sourceData.
     *
     * @param pid
     *           the pid for the object of interest
     * @return a list of datastream URI strings
     */
    public abstract List<String> getSourceData(PID pid);


    /**
     * Lists all disseminators for the pid.
     * @param pid
     * @return a list of disseminator names for the object
     */
    public abstract List<String> listDisseminators(PID pid);

    /**
     * Returns true if the object has the specified disseminator
     *
     * @param pid
     * @param dsName
     * @return
     */
    public abstract boolean hasDisseminator(PID pid, String dsName);

    /**
     * Gets datastreams that are cdr:sourceData for the object or its surrogate.
     *
     * @param pid
     * @return a list of datastream URI
     */
    public abstract List<String> getSurrogateData(PID pid);

    /**
     * Runs a generic ITQL query and returns the results as strings.
     *
     * @param query
     *           the ITQL query to run
     * @return a list of solutions, each consisting of a list of strings
     */
    public List<List<String>> queryResourceIndex(String query);

    /**
     * Verifies that the Digital Object ID existing in the triple store, returning verified PID identifier.
     *
     * @param key
     *           a key with either PID or repository path set.
     * @return a key for the same object with both PID and repository path set.
     */
    public abstract PID verify(PID key);

    /**
     * Fetches a list of all paths in every collection.
     *
     * @return a sorted list of full paths of every container in the repository.
     */
    public List<String> fetchAllCollectionPaths();

    /**
     * Looks up the label for a given PID
     *
     * @param children
     * @return
     */
    public abstract String lookupLabel(PID pid);

    public abstract String lookupLabel(String pid);

    /**
     * Looks up the slug for a given PID
     *
     * @param children
     * @return
     */
    public abstract String lookupSlug(PID pid);

    /**
     * Looks up the mimetype for the default source data
     *
     * @param pid
     * @return
     */
    public abstract String lookupSourceMimeType(PID pid);

    /**
     * Finds any PIDs which use this one as a surrogate (cdr-base:hasSurrogate)
     * @param pid
     * @return a list of PIDS which have this one as a surrogate
     */
    public abstract List<PID> fetchPIDsSurrogatefor (PID pid);

    /**
     * Sends a SPARQL query to the triple store and returns results.
     *
     * @param query
     *           the SPARQL query
     * @param format
     *           the results format, defaults to "json" on null
     * @return an Object representation of the JSON results
     */
    @SuppressWarnings("rawtypes")
    public Map sendSPARQL(String query);

    @SuppressWarnings("rawtypes")
    public Map sendSPARQL(String query, String format);

    List<String> fetchBySubjectAndPredicate(PID subject, String predicateURI);

    String fetchFirstBySubjectAndPredicate(PID subject, String predicateURI);

    String fetchState(PID pid);

    Map<String, String> fetchDisseminatorMimetypes(PID pid);

    /**
     * Retrieve a list of vocabulary objects and their associated metadata
     *
     * @return
     */
    public Map<String, Map<String, String>> fetchVocabularyInfo();

    /**
     * Retrieves a mapping of objects to their mapped vocabularies
     *
     * @return
     */
    public Map<String, Map<String, Set<String>>> fetchVocabularyMapping();

}