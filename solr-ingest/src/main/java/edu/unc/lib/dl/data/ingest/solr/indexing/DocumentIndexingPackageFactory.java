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

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;

public class DocumentIndexingPackageFactory {
	protected ManagementClient managementClient = null;

	protected AccessClient accessClient = null;

	protected int maxRetries = 2;

	protected long retryDelay = 1000L;
	
	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid) throws IndexingException {
		return this.createDocumentIndexingPackage(pid, null);
	}

	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) throws IndexingException {

		DocumentIndexingPackage dip = new DocumentIndexingPackage();
		dip.getDocument().setId(pid.getPid());
		dip.setPid(pid);
		dip.setParentDocument(parent);

		return dip;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}
}
