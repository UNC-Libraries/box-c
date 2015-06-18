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

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;

/**
 * @author bbpennel
 * @date Jun 18, 2015
 */
public class FOXMLDocumentIndexingPackageFactory extends DocumentIndexingPackageFactory {
	private static final Logger log = LoggerFactory.getLogger(FOXMLDocumentIndexingPackageFactory.class);
	
	@Override
	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) throws IndexingException {

		try {
			log.debug("Creating DIP from FOXML for {}", pid.getPid());

			Document foxml = null;
			int tries = maxRetries;
			do {

				if (tries < maxRetries) {
					Thread.sleep(retryDelay);
					log.debug("Retrieving FOXML for DIP, tries remaining: {}", tries);
				}

				try {

					foxml = managementClient.getObjectXML(pid);

				} catch (ServiceException e) {
					// If there are retries left, retry on service exception
					if (tries > 1) {
						log.warn("Failed to retrieve FOXML for " + pid.getPid() + ", retrying.", e);
					} else {
						throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid() + " after " + maxRetries
								+ " tries.", e);
					}
				}
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
}
