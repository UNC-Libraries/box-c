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
import edu.unc.lib.dl.cdr.services.exception.RecoverableServiceException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.FedoraException;
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
		LOG.debug("Called image enhancement service for " + pid.getPID());
		// get sourceData data stream IDs
		List<String> srcDSURIs = this.service.getTripleStoreQueryService().getSourceData(pid.getPID());
		Document foxml = null;
		try {
			foxml = service.getManagementClient().getObjectXML(pid.getPID());
		} catch (Exception e) {
			LOG.error("Failed to retrieve FOXML for " + pid.getPID(), e);
		}

		String mimetype = service.getTripleStoreQueryService().lookupSourceMimeType(pid.getPID());

		// get current DS version paths in iRODS
		for (String srcURI : srcDSURIs) {
			String dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);

			String dsLocation = null;
			String dsIrodsPath = null;
			String vid = null;

			try {
				Datastream ds = service.getManagementClient().getDatastream(pid.getPID(), dsid, "");
				vid = ds.getVersionID();

				// Only need to process image datastreams.
				if (mimetype.indexOf("image/") != -1) {
					LOG.debug("Image DS found: " + dsid + ", " + mimetype);

					Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, dsid);
					for (Object o : dsEl.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)) {
						if (o instanceof Element) {
							Element dsvEl = (Element) o;
							if (vid.equals(dsvEl.getAttributeValue("ID"))) {
								dsLocation = dsvEl.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS).getAttributeValue(
										"REF");
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
							String newDSID = service.getManagementClient().addManagedDatastream(pid.getPID(),
									ContentModelHelper.Datastream.IMAGE_JP2000.getName(), false, message,
									new ArrayList<String>(), "Derived JP2000 image", false, "image/jp2", convertResultURI);
						} else {
							LOG.debug("Replacing managed datastream for JP2");
							String message = "Replacing derived JP2000 image datastream.";
							service.getManagementClient().modifyDatastreamByReference(pid.getPID(),
									ContentModelHelper.Datastream.IMAGE_JP2000.getName(), false, message,
									new ArrayList<String>(), "Derived JP2000 image", "image/jp2", null, null, convertResultURI);
						}

						// Add DATA_JP2, cdr-base:derivedJP2 relation triple
						LOG.debug("Adding JP2 relationship");
						PID newDSPID = new PID(pid.getPID().getPid() + "/"
								+ ContentModelHelper.Datastream.IMAGE_JP2000.getName());
						Map<String, List<String>> rels = service.getTripleStoreQueryService().fetchAllTriples(pid.getPID());

						List<String> jp2rel = rels.get(ContentModelHelper.CDRProperty.derivedJP2.toString());
						if (jp2rel == null || !jp2rel.contains(newDSPID.getURI())) {
							//Ignore the add relation message
							service.getServicesConductor().addSideEffect(pid.getPIDString(), 
									JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.toString(), null,
									ContentModelHelper.CDRProperty.derivedJP2.toString());
							
							service.getManagementClient().addObjectRelationship(pid.getPID(),
									ContentModelHelper.CDRProperty.derivedJP2.toString(), newDSPID);
						}

						// add object model
						List<String> models = rels.get(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
						if (models == null
								|| !models.contains(ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID().getURI().toString())) {
							//Ignore the add relation message
							service.getServicesConductor().addSideEffect(pid.getPIDString(), 
									JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.toString(), null,
									ContentModelHelper.FedoraProperty.hasModel.toString());
							
							LOG.debug("Adding JP2DerivedImage content model relationship");
							service.getManagementClient().addObjectRelationship(pid.getPID(),
									ContentModelHelper.FedoraProperty.hasModel.toString(),
									ContentModelHelper.Model.JP2DERIVEDIMAGE.getPID());
						}

						// Clean up the temporary irods file
						LOG.debug("Deleting temporary jp2 Irods file");
						service.deleteIRODSFile(convertResultPath);
						LOG.debug("Finished JP2 processing");
					}
				}
			} catch (FedoraException e) {
				throw new RecoverableServiceException("Image Enhancement failed to process " + dsid, e);
			} catch (Exception e) {
				throw new EnhancementException("Image Enhancement failed to process " + dsid, e);
			}
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

	public ImageEnhancement(ImageEnhancementService service, PIDMessage pid) {
		super(pid);
		this.service = service;
	}
}
