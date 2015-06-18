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
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Creates a DocumentIndexingPackage starting from the MD_CONTENTS datastream of the object.
 * 
 * @author bbpennel
 * @date Jun 17, 2015
 */
public class MDContentsDocumentIndexingPackageFactory extends DocumentIndexingPackageFactory {
	
	private static final Logger log = LoggerFactory.getLogger(MDContentsDocumentIndexingPackageFactory.class);

	@Override
	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) throws IndexingException {
		
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
			
			dip.setParentDocument(parent);

			return dip;
		} catch (Exception e) {
			throw new IndexingException("Failed to retrieve RELS-EXT for " + pid.getPid(), e);
		}
	}
}
