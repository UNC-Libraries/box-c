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

import org.fcrepo.client.FcrepoClient;

import com.hp.hpl.jena.rdf.model.Model;

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

	public String getDepositRecordsBase() {
		return depositRecordBase;
	}

	/*
	 * returns the path for a deposit record by uuid
	 */
	public String getDepositRecordPath(String uuid) {
		return depositRecordBase + uuid;
	}

	public DepositRecord getDepositRecord(String uuid) {
		return null;
	}

	public DepositRecord createDepositRecord(String uuid, Model model) {
		return null;
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
}
