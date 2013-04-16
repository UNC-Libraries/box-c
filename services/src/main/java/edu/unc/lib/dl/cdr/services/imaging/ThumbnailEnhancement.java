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
import java.util.Collections;
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
import edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Enhancement for the creation of derivative thumbnail images.
 * @author Gregory Jansen, bbpennel
 *
 */
public class ThumbnailEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(ThumbnailEnhancement.class);

	private ThumbnailEnhancementService service = null;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called thumbnail enhancement service for " + pid);
		
		String surrogateDsUri = null;
		String surrogateDsId = null;
		PID surrogatePid = null;
		
		String dsLocation = null;
		String dsIrodsPath = null;
		String vid = null;

		try {
		// enqueues objects that use this one as a surrogate.
			List<PID> usesMeForSurrogate = this.service.getTripleStoreQueryService().fetchPIDsSurrogateFor(pid);
			for(PID usesMe: usesMeForSurrogate) {
				this.service.getMessageDirector().direct(new EnhancementMessage(usesMe, JMSMessageUtil.servicesMessageNamespace, 
						JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), ThumbnailEnhancementService.class.getName()));
				this.service.getMessageDirector().direct(new EnhancementMessage(usesMe, JMSMessageUtil.servicesMessageNamespace, 
						JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), SolrUpdateEnhancementService.class.getName()));
			}

			// get sourceData data stream IDs
			List<String> surrogateDSIDs = this.service.getTripleStoreQueryService().getSurrogateData(pid);
			if(surrogateDSIDs == null || surrogateDSIDs.size() < 1) {
				throw new EnhancementException(pid, "Cannot find a suitable DSID for making a thumbnail.");
			}
			surrogateDsUri = surrogateDSIDs.get(0);
			surrogateDsId = surrogateDsUri.substring(surrogateDsUri.lastIndexOf("/") + 1);
			surrogatePid = new PID(surrogateDsUri.substring(0,surrogateDsUri.lastIndexOf("/")));

			Document foxml = service.getManagementClient().getObjectXML(pid);

			Document surrogateFoxml = service.getManagementClient().getObjectXML(surrogatePid);

			String mimetype = service.getTripleStoreQueryService().lookupSourceMimeType(surrogatePid);
			
			Datastream ds = service.getManagementClient().getDatastream(surrogatePid, surrogateDsId, "");
			vid = ds.getVersionID();

			// Only need to process image datastreams.
			if (mimetype != null && mimetype.indexOf("image/") != -1 /* || mimetype.indexOf("application/pdf") != -1 */) {
				LOG.debug("Image DS found: " + surrogateDsId + ", " + mimetype);

				Element dsEl = FOXMLJDOMUtil.getDatastream(surrogateFoxml, surrogateDsId);
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
				LOG.debug("Source DS location: " + dsLocation);
				if (dsLocation != null) {
					dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);
					LOG.debug("Making 2 Thumbnails..");

					Map<String, List<String>> rels = service.getTripleStoreQueryService().fetchAllTriples(pid);
					List<String> thumbRels = rels.get(ContentModelHelper.CDRProperty.thumb.toString());
					{
						String dsname = ContentModelHelper.Datastream.THUMB_SMALL.getName();
						boolean exists = FOXMLJDOMUtil.getDatastream(foxml, dsname) != null;
						createStoreThumb(dsIrodsPath, 64, 64, dsname, exists, thumbRels);
					}

					{
						String dsname = ContentModelHelper.Datastream.THUMB_LARGE.getName();
						boolean exists = FOXMLJDOMUtil.getDatastream(foxml, dsname) != null;
						createStoreThumb(dsIrodsPath, 128, 128, dsname, exists, thumbRels);
					}
				}
			}
		} catch (EnhancementException e) {
			throw e;
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException("Thumbnail Enhancement failed to process, pid: " + pid.getPid() 
					+ " surrogateDS: "+surrogateDsId, e, Severity.RECOVERABLE);
		} catch (Exception e) {
			throw new EnhancementException("Thumbnail Enhancement failed to process, pid "+pid.getPid()
					+ " surrogateDS: "+surrogateDsId, e, Severity.UNRECOVERABLE);
		}

		return result;
	}

	private void createStoreThumb(String dsIrodsPath, int width, int height, String dsname, boolean exists,
			List<String> thumbRels) throws Exception {
		String resultPath = runConvertScaleStage(dsIrodsPath, "PNG", width, height);
		String resultURI = service.makeIrodsURIFromPath(resultPath);
		if (!exists) {
			String message = "adding thumbnail";
			service.getManagementClient().addManagedDatastream(pid, dsname, false, message,
					Collections.<String>emptyList(), "Thumbnail Image", false, "image/png", resultURI);
		} else {
			String message = "updating thumbnail";
			service.getManagementClient().modifyDatastreamByReference(pid, dsname, false, message,
					new ArrayList<String>(), "Thumbnail Image", "image/png", null, null, resultURI);
		}
		PID newDSPID = new PID(pid.getPid() + "/" + dsname);
		if (thumbRels == null || !thumbRels.contains(newDSPID.getURI())) {
			service.getManagementClient().addObjectRelationship(pid,
					ContentModelHelper.CDRProperty.thumb.toString(), newDSPID);
		}
		service.deleteIRODSFile(resultPath);
	}

	private String runConvertScaleStage(String dsIrodsPath, String format, int width, int height) throws Exception {
		LOG.debug("Run (image magick) convertScaleStage");
		// execute irods image magick rule
		StringBuilder arguments = new StringBuilder().append(format).append(" ").append(width).append(" ").append(height);
		InputStream response = service.remoteExecuteWithPhysicalLocation("convertScaleStage", arguments.toString(),
				dsIrodsPath);
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

	public ThumbnailEnhancement(ThumbnailEnhancementService service, PID pid) {
		super(pid);
		this.service = service;
	}
}
