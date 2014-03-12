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

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class DocumentIndexingPackageFactory {
	private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackageFactory.class);

	private ManagementClient managementClient = null;

	private AccessClient accessClient = null;

	private int maxRetries = 2;

	private long retryDelay = 1000L;

	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid) {

		try {
			log.debug("Creating DIP from FOXML for {}", pid.getPid());

			Document foxml = null;
			int tries = maxRetries;
			do {

				if (tries < maxRetries) {
					Thread.sleep(retryDelay);
					log.debug("Retrieving FOXML for DIP, tries remaining: {}", tries);
				}

				foxml = managementClient.getObjectXML(pid);
			} while (foxml == null && --tries > 0);
			if (foxml == null)
				throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid());
			return new DocumentIndexingPackage(pid, foxml);
		} catch (FedoraException e) {
			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid(), e);
		} catch (InterruptedException e) {
			throw new IndexingException("Interrupted while waiting to retry FOXML retrieval for " + pid.getPid(), e);
		}
	}

	public DocumentIndexingPackage createDocumentIndexingPackageWithRelsExt(PID pid) {

		try {
			log.debug("Creating DIP with RELS-EXT for {}", pid.getPid());

			DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);

			byte[] stream = accessClient.getDatastreamDissemination(pid, Datastream.RELS_EXT.getName(), null).getStream();
			Document relsExtDocument = ClientUtils.parseXML(stream);

			dip.setRelsExt(relsExtDocument.getRootElement());

			return dip;
		} catch (Exception e) {
			throw new IndexingException("Failed to retrieve RELS-EXT for " + pid.getPid(), e);
		}
	}

	public DocumentIndexingPackage createDocumentIndexingPackageWithMDContents(PID pid) {

		try {
			log.debug("Creating DIP with MD-CONTENTS for {}", pid.getPid());

			DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);

			try {
				byte[] stream = accessClient.getDatastreamDissemination(pid, Datastream.MD_CONTENTS.getName(), null)
						.getStream();
				Document dsDocument = ClientUtils.parseXML(stream);

				dip.setMdContents(dsDocument.getRootElement());
			} catch (NotFoundException notFound) {
				// Datastream was not found, which is okay
			}

			return dip;
		} catch (Exception e) {
			throw new IndexingException("Failed to retrieve RELS-EXT for " + pid.getPid(), e);
		}
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
