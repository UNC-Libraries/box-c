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
package edu.unc.lib.dl.data.ingest.solr.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;

/**
 * Retrieves full text data for object being indexed and stores it to the indexing document
 * @author bbpennel
 *
 */
public class SetFullTextFilter extends AbstractIndexDocumentFilter{
	private static final Logger log = LoggerFactory.getLogger(SetFullTextFilter.class);

	private AccessClient accessClient = null;

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {

		String fullTextDS = dip.getFirstTriple(CDRProperty.fullText.toString());

		if (fullTextDS == null || "false".equals(fullTextDS))
			return;

		try {
			MIMETypedStream stream = accessClient.getDatastreamDissemination(dip.getPid(), ContentModelHelper.Datastream.MD_FULL_TEXT.name(), null);
			dip.getDocument().setFullText(new String(stream.getStream()));
		} catch (FedoraException e) {
			log.error("Failed to retrieve full text datastream for {}", dip.getPid().getPid(), e);
		} catch (ServiceException e) {
			log.error("Failed to retrieve full text datastream for {}", dip.getPid().getPid(), e);
		}
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}
}
