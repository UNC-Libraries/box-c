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
package edu.unc.lib.dl.cdr.services.techmd;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.util.URIUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancement;
import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
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
 * Executes irods script which uses FITS to extract technical metadata features of objects with data file datastreams.
 * 
 * @author Gregory Jansen
 * 
 */
public class TechnicalMetadataEnhancement extends AbstractFedoraEnhancement {

	Namespace ns = JDOMNamespaceUtil.FITS_NS;

	private static final Logger LOG = LoggerFactory.getLogger(TechnicalMetadataEnhancement.class);
	private static final int MAX_EXTENSION_LENGTH = 8;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		// check to see if the service is still active
		if (!this.service.isActive()) {
			LOG.debug("{} call method exited, service is not active.", this.getClass().getName());
			return null;
		}

		String md5checksum = null;
		Map<String, Document> ds2FitsDoc = new HashMap<String, Document>();
		try {
			Document foxml = this.retrieveFoxml();
			// get sourceData data stream IDs
			List<String> srcDSURIs = this.getSourceData(foxml);
			Map<String, String> sourceMimetype = new HashMap<String, String>(srcDSURIs.size());

			for (String srcURI : srcDSURIs) { // for each source datastream
				LOG.debug("source data URI: {}", srcURI);
				String dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);
				LOG.debug("datastream ID: {}", dsid);

				// get current datastream version ID
				String dsLocation = null;
				String dsIrodsPath = null;
				String dsAltIds = null;

				Element newestSourceDS = FOXMLJDOMUtil.getMostRecentDatastream(
						ContentModelHelper.Datastream.getDatastream(dsid), foxml);
				if (newestSourceDS != null) {
					sourceMimetype.put(dsid, newestSourceDS.getAttributeValue("MIMETYPE"));
					dsLocation = newestSourceDS.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS).getAttributeValue(
							"REF");
					dsAltIds = newestSourceDS.getAttributeValue("ALT_IDS");
					Element dfDigest = newestSourceDS.getChild("contentDigest", JDOMNamespaceUtil.FOXML_NS);
					if (dfDigest != null) {
						md5checksum = dfDigest.getAttributeValue("DIGEST");
					}
				} else {
					throw new EnhancementException("Specified source datastream " + srcURI + " was not found, the object "
							+ this.pid.getPid() + " is most likely invalid", Severity.UNRECOVERABLE);
				}

				// get logical iRODS path for datastream version
				dsIrodsPath = service.getManagementClient().getIrodsPath(dsLocation);

				// call fits via irods rule for the locations
				Document fits = null;
				try {
					fits = runFITS(dsIrodsPath, dsAltIds);
				} catch (JDOMException e) {
					// Rethrow JDOM exception as an unrecoverable enhancement exception
					throw new EnhancementException(e, Severity.UNRECOVERABLE);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// put the FITS document in DS map
				ds2FitsDoc.put(dsid, fits);
			}

			// build a PREMIS document
			Document premisTech = new Document();
			Element p = new Element("premis", PREMIS_V2_NS);
			premisTech.addContent(p);
			for (String dsid : ds2FitsDoc.keySet()) {
				// get key PREMIS data
				Document fits = ds2FitsDoc.get(dsid);
				String size = fits.getRootElement().getChild("fileinfo", FITS_NS).getChildText("size", FITS_NS);
				
				String fedoraMimetype = sourceMimetype.get(dsid);
				String fitsMimetype = fedoraMimetype;
				// Format only provided if using FITS mimetype, since otherwise it may not match
				String format = null;
				
				// If fedora mimetype is not meaningful, then override with FITS generated value
				if (fedoraMimetype == null || fedoraMimetype.trim().length() == 0
						|| fedoraMimetype.contains("octet-stream")) {
					
					// IDENTIFICATION LOGIC
					// get mimetype out of FITS XML
					Element trustedIdentity = null;
					Element idn = fits.getRootElement().getChild("identification", ns);
					for (Object child : idn.getChildren("identity", ns)) {
						Element el = (Element) child;
						if (idn.getAttributeValue("status") == null
								|| el.getChildren("tool", ns).size() > 1
								|| (!"Exiftool".equals(el.getChild("tool", ns).getAttributeValue("toolname")) && !"application/x-symlink"
										.equals(el.getAttributeValue("mimetype")))) {
							trustedIdentity = el;
							break;
						}
					}

					if (trustedIdentity != null) {
						fitsMimetype = trustedIdentity.getAttributeValue("mimetype");
						format = trustedIdentity.getAttributeValue("format");
					} else {
						format = "Unknown";
						LOG.warn("FITS unable to conclusively identify file: {}/{}", pid, dsid);
						LOG.info(new XMLOutputter().outputString(fits));
					}
				}

				if ("DATA_FILE".equals(dsid)) {
					if (fitsMimetype != null) {
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceMimeType.getPredicate(),
								ContentModelHelper.CDRProperty.hasSourceMimeType.getNamespace(), fitsMimetype, null, foxml);
					} else { // application/octet-stream
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceMimeType.getPredicate(),
								ContentModelHelper.CDRProperty.hasSourceMimeType.getNamespace(), "application/octet-stream",
								null, foxml);
					}

					try {
						Long.parseLong(size);
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceFileSize.getPredicate(),
								ContentModelHelper.CDRProperty.hasSourceFileSize.getNamespace(), size,
								"http://www.w3.org/2001/XMLSchema#long", foxml);
					} catch (NumberFormatException e) {
						LOG.error("FITS produced a non-integer value for size: " + size);
					}
				}

				Element objCharsEl = new Element("objectCharacteristics", PREMIS_V2_NS);
				if (md5checksum != null) {
					objCharsEl.addContent(
							new Element("fixity", PREMIS_V2_NS).addContent(
									new Element("messageDigestAlgorithm", PREMIS_V2_NS).setText("MD5"))
									.addContent(new Element("messageDigest", PREMIS_V2_NS).setText(md5checksum)));
				}
				if (format != null) {
					objCharsEl.addContent(
							new Element("format", PREMIS_V2_NS)
								.addContent(new Element("formatDesignation", PREMIS_V2_NS)
								.addContent(new Element("formatName", PREMIS_V2_NS)
								.setText(format))));
				}

				p.addContent(new Element("object", PREMIS_V2_NS)
						.addContent(
								new Element("objectIdentifier", PREMIS_V2_NS).addContent(
										new Element("objectIdentifierType", PREMIS_V2_NS).setText("Fedora Datastream PID"))
										.addContent(new Element("objectIdentifierValue", PREMIS_V2_NS).setText(dsid)))
						.addContent(
								objCharsEl
										.addContent(new Element("compositionLevel", PREMIS_V2_NS).setText("0"))
										.addContent(new Element("size", PREMIS_V2_NS).setText(size))
										.addContent(
												new Element("objectCharacteristicsExtension", PREMIS_V2_NS).addContent(ds2FitsDoc
														.get(dsid).detachRootElement())))
						.setAttribute("type", PREMIS_V2_NS.getPrefix() + ":file", JDOMNamespaceUtil.XSI_NS));
			}

			// upload tech MD PREMIS XML
			String premisTechURL = service.getManagementClient().upload(premisTech);

			// Add or replace the MD_TECHNICAL datastream for the object
			if (FOXMLJDOMUtil.getDatastream(foxml, ContentModelHelper.Datastream.MD_TECHNICAL.getName()) == null) {
				LOG.debug("Adding FITS output to MD_TECHNICAL");
				String message = "Adding technical metadata derived by FITS";
				service.getManagementClient().addManagedDatastream(pid,
						ContentModelHelper.Datastream.MD_TECHNICAL.getName(), false, message, new ArrayList<String>(),
						"PREMIS Technical Metadata", false, "text/xml", premisTechURL);
			} else {
				LOG.debug("Replacing MD_TECHNICAL with new FITS output");
				String message = "Replacing technical metadata derived by FITS";
				service.getManagementClient().modifyDatastreamByReference(pid,
						ContentModelHelper.Datastream.MD_TECHNICAL.getName(), false, message, new ArrayList<String>(),
						"PREMIS Technical Metadata", "text/xml", null, null, premisTechURL);
			}

			LOG.debug("Adding techData relationship");
			PID newDSPID = new PID(pid.getPid() + "/" + ContentModelHelper.Datastream.MD_TECHNICAL.getName());
			Map<String, List<String>> rels = service.getTripleStoreQueryService().fetchAllTriples(pid);

			List<String> techrel = rels.get(ContentModelHelper.CDRProperty.techData.toString());
			if (techrel == null || !techrel.contains(newDSPID.getURI())) {
				service.getManagementClient().addObjectRelationship(pid,
						ContentModelHelper.CDRProperty.techData.toString(), newDSPID);
			}

			LOG.debug("Finished MD_TECHNICAL updating for {}", pid.getPid());
		} catch (FileSystemException e) {
			throw new EnhancementException(e, Severity.FATAL);
		} catch (NotFoundException e) {
			throw new EnhancementException(e, Severity.UNRECOVERABLE);
		} catch (FedoraException e) {
			throw new EnhancementException(e, Severity.RECOVERABLE);
		}
		return result;
	}

	/**
	 * Executes fits extract irods script
	 * 
	 * @param dsIrodsPath
	 * @return FITS output XML Document
	 */
	private Document runFITS(String dsIrodsPath, String altIds) throws Exception {
		Document result = null;

		// try to extract file name from ALT_ID
		String filename = null;
		if (altIds != null) {
			for (String altid : altIds.split(" ")) {
				if (altid.length() > 0) {
					String rawPath = altid;
					// Narrow file name down to after the last /
					int lastSlash = rawPath.lastIndexOf("/");
					if (lastSlash > 0)
						rawPath = rawPath.substring(lastSlash + 1);
					int ind = rawPath.lastIndexOf(".");
					// Use text after last . as extension if its length is 0 > len >= MAX_EXTENSION_LENGTH
					if (ind > 0 && rawPath.length() - 1 > ind && (rawPath.length() - ind <= MAX_EXTENSION_LENGTH)) {
						filename = rawPath.substring(ind + 1);
						filename = URIUtil.decode("linkedfile." + filename);
						break;
					}
				}
			}
		}

		// execute FITS
		LOG.debug("Run fits for {}", dsIrodsPath);
		BufferedReader reader = null;
		String xmlstr = null;
		String errstr = null;
		try {
			if (filename == null) {
				reader = new BufferedReader(new InputStreamReader(
						((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation("fitsextract",
								dsIrodsPath)));
			} else {
				reader = new BufferedReader(new InputStreamReader(
						((AbstractIrodsObjectEnhancementService) service).remoteExecuteWithPhysicalLocation("fitsextract",
								"'" + filename + "'", dsIrodsPath)));
			}
			StringBuilder xml = new StringBuilder();
			StringBuilder err = new StringBuilder();
			boolean declareReached = false;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (!declareReached && line.startsWith("<?xml")) {
					declareReached = true;
				}
				if (declareReached) {
					xml.append(line).append("\n");
				} else {
					if (line.trim().length() > 0) {
						err.append(line).append("\n");
					}
				}
			}
			xmlstr = xml.toString();
			errstr = err.toString();
			if (errstr.length() > 0) {
				LOG.warn("FITS is warning for path: " + dsIrodsPath);
				LOG.info(errstr);
			}
			result = new SAXBuilder().build(new StringReader(xmlstr));
			return result;
		} catch (JDOMException e) {
			LOG.warn("Failed to parse FITS output for path: " + dsIrodsPath);
			LOG.info("FITS returned: \n" + xmlstr + "\n\n" + errstr);
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	public TechnicalMetadataEnhancement(AbstractFedoraEnhancementService service, EnhancementMessage message) {
		super(service, message);
	}
}
