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

import org.fcrepo.client.FcrepoClient;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Client for interacting with a fedora repository and obtaining objects
 * contained in it
 * 
 * @author bbpennel
 *
 */
public class Repository {
	private String depositRecordBase;
	private String vocabulariesBase;
	private String contentBase;
	private String agentsBase;
	private String policiesBase;

	private FcrepoClient client;

	private String baseHost;

	private String fedoraBase;

	private String authUsername;

	private String authPassword;

	private String authHost;
	
	private RepositoryObjectFactory repositoryFactory;
	
	private RepositoryObjectDataLoader repositoryObjectDataLoader;

	/**
	 * Retrieves an existing DepositRecord object
	 * 
	 * @param pid
	 * @return
	 * @throws FedoraException
	 */
	public DepositRecord getDepositRecord(PID pid) throws FedoraException {
		DepositRecord record = new DepositRecord(pid, this, repositoryObjectDataLoader);
		
		// Verify that the retrieved object is a deposit record
		return record.validateType();
	}

	/**
	 * Creates a new deposit record object with the given uuid.
	 * Properties in the supplied model will be added to the deposit record. 
	 * 
	 * @param pid
	 * @param model
	 * @return
	 * @throws FedoraException
	 */
	public DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException {
		URI depositRecordUri = repositoryFactory.createDepositRecord(pid.getRepositoryUri(), model);
		// Create a new pid just in case fedora didn't agree to the suggested one
		PID newPid = PIDs.get(depositRecordUri);
		
		DepositRecord depositRecord = new DepositRecord(newPid, this, repositoryObjectDataLoader);
		return depositRecord;
	}

	public String getVocabulariesBase() {
		return vocabulariesBase;
	}

	public String getContentBase() {
		return contentBase;
	}

	public String getContentPath(PID pid) {
		return null;
	}

	public ContentObject getContentObject(PID pid) {
		return null;
	}

	public ContentObject getContentObject(String path) {
		return null;
	}

	public String getAgentsBase() {
		return agentsBase;
	}

	public String getPoliciesBase() {
		return policiesBase;
	}

	public String getDepositRecordBase() {
		return depositRecordBase;
	}

	public void setDepositRecordBase(String depositRecordBase) {
		this.depositRecordBase = depositRecordBase;
	}

	public void setVocabulariesBase(String vocabulariesBase) {
		this.vocabulariesBase = vocabulariesBase;
	}

	public void setContentBase(String contentBase) {
		this.contentBase = contentBase;
	}

	public void setAgentsBase(String agentsBase) {
		this.agentsBase = agentsBase;
	}

	public void setPoliciesBase(String policiesBase) {
		this.policiesBase = policiesBase;
	}

	public String getBaseHost() {
		return baseHost;
	}

	public void setBaseHost(String baseHost) {
		this.baseHost = baseHost;
	}

	public String getFedoraBase() {
		return fedoraBase;
	}

	public void setFedoraBase(String fedoraBase) {
		this.fedoraBase = fedoraBase;
	}

	public String getAuthUsername() {
		return authUsername;
	}

	public void setAuthUsername(String authUsername) {
		this.authUsername = authUsername;
	}

	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}

	public String getAuthHost() {
		return authHost;
	}

	public void setAuthHost(String authHost) {
		this.authHost = authHost;
	}

	public void setClient(FcrepoClient client) {
		this.client = client;
	}

	public FcrepoClient getClient() {

		if (client == null) {
			client = FcrepoClient.client().credentials(authUsername, authPassword).authScope(authHost)
					.throwExceptionOnFailure().build();
		}
		return client;
	}

	public RepositoryObjectDataLoader getRepositoryObjectDataLoader() {
		return repositoryObjectDataLoader;
	}

	public void setRepositoryObjectDataLoader(RepositoryObjectDataLoader repositoryObjectDataLoader) {
		this.repositoryObjectDataLoader = repositoryObjectDataLoader;
	}

	public RepositoryObjectFactory getRepositoryObjectFactory() {
		return repositoryFactory;
	}

	public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
		this.repositoryFactory = repositoryObjectFactory;
	}
}
