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
package edu.unc.lib.dl.cdr.services.imaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
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
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Enhancement class for the construction of a jp2 derived datastream based off of all image data_file datastreams
 * attached to the specified object.
 * 
 * @author bbpennel
 */
public class ImageEnhancement extends AbstractFedoraEnhancement {
	private static final Logger LOG = LoggerFactory.getLogger(ImageEnhancement.class);

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

				String dsLocation = null;
				String dsIrodsPath = null;

				dsLocation = newestSourceDS.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS).getAttributeValue(
						"REF");

				LOG.debug("Image DS location: {}", dsLocation);
				if (dsLocation != null) {
					dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);
					// Ask irods to make the jp2 object
					LOG.debug("Convert to JP2");
					String convertResultPath = runConvertJP2(dsIrodsPath);

					String convertResultURI = ((AbstractIrodsObjectEnhancementService) service)
							.makeIrodsURIFromPath(convertResultPath);
					LOG.debug("attempting to ingest conversion result: " + convertResultPath);

					boolean exists = service.getManagementClient()
							.getDatastream(pid, Datastream.IMAGE_JP2000.getName()) != null;
					if (exists) {
						LOG.debug("Replacing managed datastream for JP2");
						String message = "Replacing derived JP2000 image datastream.";
						service.getManagementClient().modifyDatastreamByReference(pid,
								Datastream.IMAGE_JP2000.getName(), false, message,
								new ArrayList<String>(), "Derived JP2000 image", "image/jp2", null, null, convertResultURI);
					} else {
						LOG.debug("Adding managed datastream for JP2");
						String message = "Adding derived JP2000 image datastream.";
						service.getManagementClient().addManagedDatastream(pid,
								Datastream.IMAGE_JP2000.getName(), false, message,
								new ArrayList<String>(), "Derived JP2000 image", false, "image/jp2", convertResultURI);
					}

					// Add DATA_JP2, cdr-base:derivedJP2 relation triple
					LOG.debug("Adding JP2 relationship");
					PID newDSPID = new PID(pid.getPid() + "/" + ContentModelHelper.Datastream.IMAGE_JP2000.getName());
					Map<String, List<String>> rels = service.getTripleStoreQueryService().fetchAllTriples(pid);

					List<String> jp2rel = rels.get(ContentModelHelper.CDRProperty.derivedJP2.toString());
					if (jp2rel == null || !jp2rel.contains(newDSPID.getURI())) {
						service.getManagementClient().addObjectRelationship(pid,
								ContentModelHelper.CDRProperty.derivedJP2.toString(), newDSPID);
					}

					// add object model
					List<String> models = rels.get(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
					if (models == null
							|| !models.contains(ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID().getURI().toString())) {
						LOG.debug("Adding JP2DerivedImage content model relationship");
						service.getManagementClient().addObjectRelationship(pid,
								ContentModelHelper.FedoraProperty.hasModel.toString(),
								ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID());
					}

					// Clean up the temporary irods file
					LOG.debug("Deleting temporary jp2 Irods file");
					((AbstractIrodsObjectEnhancementService) service).deleteIRODSFile(convertResultPath);
					LOG.debug("Finished JP2 processing");
				}
			}
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException("Image Enhancement failed to process " + dsid, e, Severity.RECOVERABLE);
		} catch (Exception e) {
			throw new EnhancementException("Image Enhancement failed to process " + dsid, e, Severity.UNRECOVERABLE);
		}

		return result;
	}

	private String runConvertJP2(String dsIrodsPath) throws Exception {
		LOG.debug("Run (image magick) convertjp2 {}", dsIrodsPath);
		// execute irods image magick rule
		InputStream response = ((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation(
				"convertjp2", dsIrodsPath);
		try(BufferedReader r = new BufferedReader(new InputStreamReader(response))) {
			return r.readLine().trim();
		} catch (Exception e) {
			throw e;
		}
	}

	public ImageEnhancement(ImageEnhancementService service, EnhancementMessage message) {
		super(service, message);
	}
}
