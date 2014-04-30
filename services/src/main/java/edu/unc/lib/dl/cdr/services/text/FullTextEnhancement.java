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
package edu.unc.lib.dl.cdr.services.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancement;
import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Enhancement which extracts the full text from sourceData datastreams of supported mimetypes. The text is stored to
 * MD_FULL_TEXT datastream of the object. A fullText relation is also added to the object to record which objects have
 * had full text extracted and which datastream it is stored in.
 * 
 * @author bbpennel
 * 
 */
public class FullTextEnhancement extends AbstractFedoraEnhancement {
	private static final Logger LOG = LoggerFactory.getLogger(FullTextEnhancement.class);

	public FullTextEnhancement(FullTextEnhancementService service, EnhancementMessage message) {
		super(service, message);
	}

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called image enhancement service for {}", pid);

		String dsid = null;
		try {
			Document foxml = this.retrieveFoxml();
			// get sourceData data stream IDs
			List<String> srcDSURIs = this.getSourceData(foxml);

			// get current DS version paths in iRODS
			for (String srcURI : srcDSURIs) {
				dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);

				Element newestSourceDS = FOXMLJDOMUtil.getMostRecentDatastream(
						ContentModelHelper.Datastream.getDatastream(dsid), foxml);
				
				if (newestSourceDS == null)
					throw new EnhancementException("Specified source datastream " + srcURI + " was not found, the object "
							+ this.pid.getPid() + " is most likely invalid", Severity.UNRECOVERABLE);

				String dsLocation = newestSourceDS.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS)
						.getAttributeValue("REF");
				String dsIrodsPath = null;

				if (dsLocation != null) {
					dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);

					String text = this.extractText(dsIrodsPath);
					
					// Instead of adding an empty full text DS, add flag to indicate this object has nothing to extract some e
					if (text == null || text.trim().length() == 0) {
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.fullText.getPredicate(),
								ContentModelHelper.CDRProperty.fullText.getNamespace(), 
								"false", null, foxml);
						continue;
					}

					// Add full text ds to object
					String textURL = service.getManagementClient().upload(text);

					if (FOXMLJDOMUtil.getDatastream(foxml, ContentModelHelper.Datastream.MD_FULL_TEXT.getName()) == null) {
						String message = "Adding full text metadata extracted by Apache Tika";
						service.getManagementClient().addManagedDatastream(pid,
								ContentModelHelper.Datastream.MD_FULL_TEXT.getName(), false, message, new ArrayList<String>(),
								ContentModelHelper.Datastream.MD_FULL_TEXT.getLabel(), false, "text/plain", textURL);
					} else {
						String message = "Replacing full text metadata extracted by Apache Tika";
						service.getManagementClient().modifyDatastreamByReference(pid,
								ContentModelHelper.Datastream.MD_FULL_TEXT.getName(), false, message, new ArrayList<String>(),
								ContentModelHelper.Datastream.MD_FULL_TEXT.getLabel(), "text/plain", null, null, textURL);
					}

					// Add full text relation
					PID textPID = new PID(pid.getPid() + "/" + ContentModelHelper.Datastream.MD_FULL_TEXT.getName());
					setExclusiveTripleRelation(pid, ContentModelHelper.CDRProperty.fullText.getPredicate(),
							ContentModelHelper.CDRProperty.fullText.getNamespace(), textPID, foxml);
				}
			}
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException("Full Text Enhancement failed to process " + dsid, e, Severity.RECOVERABLE);
		} catch (Exception e) {
			throw new EnhancementException("Full Text Enhancement failed to process " + dsid, e, Severity.UNRECOVERABLE);
		}

		return result;
	}

	private String extractText(String dsIrodsPath) throws Exception {
		LOG.debug("Run irods script to perform text extraction on {} ", dsIrodsPath);
		InputStream response = ((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation(
				"textextract", dsIrodsPath);
		BufferedReader r = new BufferedReader(new InputStreamReader(response));
		try {
			StringBuilder text = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				text.append(line);
			}
			return text.toString().trim();
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				r.close();
			} catch (Exception ignored) {
			}
		}
	}
}
