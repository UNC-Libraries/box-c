/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import java.net.URI;
import java.util.Date;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

/**
 * A generic repository object, with properties common to objects in the repository.
 * 
 * @author bbpennel
 *
 */
public abstract class RepositoryObject {

	// Repository which produced and manages this object
	protected Repository repository;
	// Loader for lazy loading data about this object when requested
	protected RepositoryObjectDataLoader dataLoader;

	// The identifier and path information for this object
	protected PID pid;

	protected Model model;

	protected Date lastModified;
	protected String etag;

	protected List<String> types;

	protected RepositoryObject(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		this.repository = repository;
		this.pid = pid;
		this.dataLoader = dataLoader;
	}

	/**
	 * Get the URI of this object in the repository
	 * 
	 * @return
	 */
	public URI getUri() {
		return pid.getRepositoryUri();
	}

	/**
	 * Get a model containing properties and relations held by this object. 
	 * 
	 * @return
	 * @throws FedoraException
	 */
	public Model getModel() throws FedoraException {
		if (model == null) {
			dataLoader.loadModel(this);
		}

		return model;
	}

	/**
	 * Set the model
	 * 
	 * @param model
	 * @return
	 */
	public RepositoryObject setModel(Model model) {
		this.model = model;
		return this;
	}

	public Resource getResource() throws FedoraException {
		return getModel().getResource(getUri().toString());
	}

	/**
	 * Adds each event in the provided model to this object.
	 * 
	 * @param model
	 * @return
	 */
	public RepositoryObject addPremisEvents(Model model) {
		// TODO
		return this;
	}

	/**
	 * Returns true if this object is assigned the given RDF type
	 * 
	 * @param type
	 *            URI for the RDF type being checked for
	 * @return
	 * @throws FedoraException
	 */
	protected boolean isType(String type) throws FedoraException {
		return getTypes().contains(type);
	}

	/**
	 * Throws a Fedora exception if the object does not match the expected RDF types
	 * 
	 * @return
	 * @throws FedoraException
	 */
	public abstract RepositoryObject validateType() throws FedoraException;

	/**
	 * Get the PID of this object
	 * 
	 * @return
	 */
	public PID getPid() {
		return pid;
	}

	/**
	 * Set the PID
	 * 
	 * @param pid
	 */
	public void setPid(PID pid) {
		this.pid = pid;
	}

	/**
	 * Get the last modified date
	 * 
	 * @return
	 */
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * Set the last modified date
	 * 
	 * @param lastModified
	 */
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Get the ETag representing the version of the object retrieved
	 * 
	 * @return
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * Set the etag
	 * 
	 * @param etag
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/**
	 * Get a list of the RDF types for this object
	 * 
	 * @return
	 * @throws FedoraException
	 */
	public List<String> getTypes() throws FedoraException {
		if (types == null) {
			dataLoader.loadTypes(this);
		}
		return types;
	}

	/**
	 * Set the list of RDF types
	 * 
	 * @param types
	 */
	public void setTypes(List<String> types) {
		this.types = types;
	}

	/**
	 * The URI where RDF metadata about this object can be retrieved from.
	 * 
	 * @return
	 */
	public URI getMetadataUri() {
		return pid.getRepositoryUri();
	}
}
