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

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancement;
import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Enhancement for the creation of derivative thumbnail images.
 *
 * @author Gregory Jansen
 * @author bbpennel
 *
 */
public class ThumbnailEnhancement extends AbstractFedoraEnhancement {
	private static final Logger LOG = LoggerFactory.getLogger(ThumbnailEnhancement.class);

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		long startJob = System.currentTimeMillis();
		LOG.debug("Called thumbnail enhancement service for {}", pid);

		String surrogateDsUri = null;
		String surrogateDsId = null;
		PID surrogatePid = null;

		String dsLocation = null;
		String dsIrodsPath = null;

		try {
			// enqueues objects that use this one as a surrogate.
			List<PID> usesMeForSurrogate = this.service.getTripleStoreQueryService().fetchPIDsSurrogateFor(pid);
			for (PID usesMe : usesMeForSurrogate) {
				this.service.getMessageDirector().direct(
						new EnhancementMessage(usesMe, JMSMessageUtil.servicesMessageNamespace,
								JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), ThumbnailEnhancementService.class
										.getName()));
				this.service.getMessageDirector().direct(
						new EnhancementMessage(usesMe, JMSMessageUtil.servicesMessageNamespace,
								JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), SolrUpdateEnhancementService.class
										.getName()));
			}

			// get sourceData data stream IDs
			List<String> surrogateDSIDs = this.service.getTripleStoreQueryService().getSurrogateData(pid);
			if (surrogateDSIDs == null || surrogateDSIDs.size() < 1) {
				throw new EnhancementException(pid, "Cannot find a suitable DSID for making a thumbnail.");
			}
			surrogateDsUri = surrogateDSIDs.get(0);
			surrogateDsId = surrogateDsUri.substring(surrogateDsUri.lastIndexOf("/") + 1);
			surrogatePid = new PID(surrogateDsUri.substring(0, surrogateDsUri.lastIndexOf("/")));

			Document foxml = this.retrieveFoxml();
			Document surrogateFoxml;
			// Only retrieve the surrogate FOXML if the surrogate is a different object
			if (surrogatePid.equals(message.getPid())) {
				surrogateFoxml = foxml;
			} else {
				surrogateFoxml = client.getObjectXML(surrogatePid);
			}

			Element newestSourceDS = FOXMLJDOMUtil.getMostRecentDatastream(
					ContentModelHelper.Datastream.getDatastream(surrogateDsId), surrogateFoxml);

			if (newestSourceDS == null)
				throw new EnhancementException("Specified source or surrogate datastream " + surrogateDsUri
						+ " was not found, the object " + this.pid.getPid() + " is most likely invalid",
						Severity.UNRECOVERABLE);

			dsLocation = newestSourceDS.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS).getAttributeValue("REF");

			LOG.debug("Source DS location: {}", dsLocation);
			if (dsLocation != null) {
				dsIrodsPath = client.getIrodsPath(dsLocation);
				LOG.debug("Making 2 Thumbnails..");

				List<String> thumbRels = FOXMLJDOMUtil.getRelationValues(
						ContentModelHelper.CDRProperty.thumb.getPredicate(), JDOMNamespaceUtil.CDR_NS,
						FOXMLJDOMUtil.getRelsExt(foxml));
				{
					String dsname = ContentModelHelper.Datastream.THUMB_SMALL.getName();
					boolean exists = client.dataStreamExists(pid, dsname);
					createStoreThumb(dsIrodsPath, 64, 64, dsname, exists, thumbRels);
				}

				{
					String dsname = ContentModelHelper.Datastream.THUMB_LARGE.getName();
					boolean exists = client.dataStreamExists(pid, dsname);
					createStoreThumb(dsIrodsPath, 128, 128, dsname, exists, thumbRels);
				}
			}
			LOG.debug("Finished THUMB updating for {} in {}ms", pid.getPid(), (System.currentTimeMillis() - startJob));
		} catch (EnhancementException e) {
			throw e;
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException("Thumbnail Enhancement failed to process, pid: " + pid.getPid()
					+ " surrogateDS: " + surrogateDsId, e, Severity.RECOVERABLE);
		} catch (Exception e) {
			throw new EnhancementException("Thumbnail Enhancement failed to process, pid " + pid.getPid()
					+ " surrogateDS: " + surrogateDsId, e, Severity.UNRECOVERABLE);
		}

		return result;
	}

	private void createStoreThumb(String dsIrodsPath, int width, int height, String dsname, boolean exists,
			List<String> thumbRels) throws Exception {
		long convertStart = System.currentTimeMillis();
		String resultPath = runConvertScaleStage(dsIrodsPath, "PNG", width, height);
		LOG.debug("Generated {} image in {}ms", dsname, (System.currentTimeMillis() - convertStart));
		
		String resultURI = ((AbstractIrodsObjectEnhancementService) service).makeIrodsURIFromPath(resultPath);
		long addDsStart = System.currentTimeMillis();
		if (!exists) {
			String message = "adding thumbnail";
			client.addManagedDatastream(pid, dsname, false, message,
					Collections.<String> emptyList(), "Thumbnail Image", false, "image/png", resultURI);
		} else {
			String message = "updating thumbnail";
			client.modifyDatastreamByReference(pid, dsname, false, message,
					new ArrayList<String>(), "Thumbnail Image", "image/png", null, null, resultURI);
		}
		LOG.debug("Added {} datastream in {}ms", dsname, (System.currentTimeMillis() - addDsStart));

		long deleteStart = System.currentTimeMillis();
		((AbstractIrodsObjectEnhancementService) service).deleteIRODSFile(resultPath);
		LOG.debug("Cleaned up irods file in {}ms", dsname, (System.currentTimeMillis() - deleteStart));
	}

	private String runConvertScaleStage(String dsIrodsPath, String format, int width, int height) throws Exception {
		LOG.debug("Run (image magick) convertScaleStage");
		// execute irods image magick rule
		StringBuilder arguments = new StringBuilder().append(format).append(" ").append(width).append(" ").append(height);
		InputStream response = ((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation(
				"convertScaleStage", arguments.toString(), dsIrodsPath);
		try(BufferedReader r = new BufferedReader(new InputStreamReader(response))) {
			return r.readLine().trim();
		} catch (Exception e) {
			throw e;
		}
	}

	public ThumbnailEnhancement(ThumbnailEnhancementService service, EnhancementMessage message) {
		super(service, message);
	}
}
