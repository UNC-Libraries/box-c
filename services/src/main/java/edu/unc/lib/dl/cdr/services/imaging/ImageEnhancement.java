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

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Enhancement class for the construction of a jp2 derived datastream based off of all image data_file datastreams
 * attached to the specified object.
 * 
 * @author bbpennel
 */
public class ImageEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(ImageEnhancement.class);

	private ImageEnhancementService service = null;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called image enhancement service for " + pid.getPid());

		Document foxml = null;
		String dsid = null;
		try {
			// get sourceData data stream IDs
			List<String> srcDSURIs = this.service.getTripleStoreQueryService().getSourceData(pid.getPid());

			foxml = service.getManagementClient().getObjectXML(pid.getPid());
			String mimetype = service.getTripleStoreQueryService().lookupSourceMimeType(pid.getPid());

			// get current DS version paths in iRODS
			for (String srcURI : srcDSURIs) {
				dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);

				String dsLocation = null;
				String dsIrodsPath = null;
				String vid = null;

				Datastream ds = service.getManagementClient().getDatastream(pid.getPid(), dsid, "");
				vid = ds.getVersionID();

				// Only need to process image datastreams.
				if (mimetype.indexOf("image/") != -1) {
					LOG.debug("Image DS found: " + dsid + ", " + mimetype);

					Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, dsid);
					for (Object o : dsEl.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)) {
						if (o instanceof Element) {
							Element dsvEl = (Element) o;
							if (vid.equals(dsvEl.getAttributeValue("ID"))) {
								dsLocation = dsvEl.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS)
										.getAttributeValue("REF");
								break;
							}
						}
					}
					LOG.debug("Image DS location: " + dsLocation);
					if (dsLocation != null) {
						dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);
						// Ask irods to make the jp2 object
						LOG.debug("Convert to JP2");
						String convertResultPath = runConvertJP2(dsIrodsPath);

						String convertResultURI = service.makeIrodsURIFromPath(convertResultPath);
						LOG.debug("attempting to ingest conversion result: " + convertResultPath);

						if (FOXMLJDOMUtil.getDatastream(foxml, ContentModelHelper.Datastream.IMAGE_JP2000.getName()) == null) {
							// Add the datastream for the new derived jp2
							LOG.debug("Adding managed datastream for JP2");
							String message = "Adding derived JP2000 image datastream.";
							service.getManagementClient().addManagedDatastream(pid.getPid(),
									ContentModelHelper.Datastream.IMAGE_JP2000.getName(), false, message,
									new ArrayList<String>(), "Derived JP2000 image", false, "image/jp2", convertResultURI);
						} else {
							LOG.debug("Replacing managed datastream for JP2");
							String message = "Replacing derived JP2000 image datastream.";
							service.getManagementClient().modifyDatastreamByReference(pid.getPid(),
									ContentModelHelper.Datastream.IMAGE_JP2000.getName(), false, message,
									new ArrayList<String>(), "Derived JP2000 image", "image/jp2", null, null,
									convertResultURI);
						}

						// Add DATA_JP2, cdr-base:derivedJP2 relation triple
						LOG.debug("Adding JP2 relationship");
						PID newDSPID = new PID(pid.getTargetID() + "/"
								+ ContentModelHelper.Datastream.IMAGE_JP2000.getName());
						Map<String, List<String>> rels = service.getTripleStoreQueryService()
								.fetchAllTriples(pid.getPid());

						List<String> jp2rel = rels.get(ContentModelHelper.CDRProperty.derivedJP2.toString());
						if (jp2rel == null || !jp2rel.contains(newDSPID.getURI())) {
							service.getManagementClient().addObjectRelationship(pid.getPid(),
									ContentModelHelper.CDRProperty.derivedJP2.toString(), newDSPID);
						}

						// add object model
						List<String> models = rels.get(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
						if (models == null
								|| !models.contains(ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID().getURI().toString())) {
							LOG.debug("Adding JP2DerivedImage content model relationship");
							service.getManagementClient().addObjectRelationship(pid.getPid(),
									ContentModelHelper.FedoraProperty.hasModel.toString(),
									ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID());
						}

						// Clean up the temporary irods file
						LOG.debug("Deleting temporary jp2 Irods file");
						service.deleteIRODSFile(convertResultPath);
						LOG.debug("Finished JP2 processing");
					}
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
		LOG.debug("Run (image magick) convertjp2");
		// execute irods image magick rule
		InputStream response = service.remoteExecuteWithPhysicalLocation("convertjp2", dsIrodsPath);
		BufferedReader r = new BufferedReader(new InputStreamReader(response));
		try {
			return r.readLine().trim();
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				r.close();
			} catch (Exception ignored) {
			}
		}
	}

	public ImageEnhancement(ImageEnhancementService service, EnhancementMessage pid) {
		super(pid);
		this.service = service;
	}
}
