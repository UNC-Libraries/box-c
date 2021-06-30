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
package edu.unc.lib.boxc.model.api.objects;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.event.PremisLogger;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * A generic repository object, with properties common to objects in the repository.
 * @author bbpennel
 */
public interface RepositoryObject {

    /**
     * Get the URI of this object in the repository
     *
     * @return
     */
    URI getUri();

    /**
     * Get a model containing properties and relations held by this object.
     *
     * @return
     * @throws FedoraException
     */
    Model getModel() throws FedoraException;

    Model getModel(boolean checkForUpdates) throws FedoraException;

    /**
     * Return true if the model for this object has been populated
     *
     * @return
     */
    boolean hasModel();

    /**
     * Store the relationships and properties belonging to this object
     *
     * @param model
     * @return
     */
    void storeModel(Model model);

    Resource getResource() throws FedoraException;

    Resource getResource(boolean checkForUpdates) throws FedoraException;

    /**
     * Get the PREMIS event log for this object
     *
     * @return
     */
    PremisLogger getPremisLog();

    /**
     * @return the ResourceType which describes this object
     */
    ResourceType getResourceType();

    /**
     * Throws a Fedora exception if the object does not match the expected RDF types
     *
     * @return
     * @throws FedoraException
     */
    RepositoryObject validateType() throws FedoraException;

    /**
     * Get the PID of this object
     *
     * @return
     */
    PID getPid();

    /**
     * Get the parent of the current object
     * @return
     */
    RepositoryObject getParent();

    /**
     * @return pid of the parent of the current object
     */
    PID getParentPid();

    /**
     * Get the last modified date
     *
     * @return
     */
    Date getLastModified();

    /**
     * Get the creation date for this object
     * @return
     */
    Date getCreatedDate();

    /**
     * Get the ETag representing the version of the object retrieved
     *
     * @return
     */
    String getEtag();

    /**
     * Get a list of the RDF types for this object
     *
     * @return
     * @throws FedoraException
     */
    List<String> getTypes() throws FedoraException;

    /**
     * @param type URI representation of type
     * @return Returns true if this object is of the requested type
     * @throws FedoraException
     */
    default boolean isType(String type) throws FedoraException {
        return getTypes().contains(type);
    }

    /**
     * The URI where RDF metadata about this object can be retrieved from.
     *
     * @return
     */
    URI getMetadataUri();

    /**
     * Returns true if this object  is unmodified according to etag by
     * verifying if the locally held etag matches the current one in the
     * repository
     *
     * @return
     */
    boolean isUnmodified();

    /**
     * Indicate that the state of this object should be refreshed
     */
    void shouldRefresh();

}