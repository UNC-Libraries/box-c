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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.util.URIUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Executes irods script which uses FITS to extract technical metadata features of objects with data file datastreams.
 * 
 * @author Gregory Jansen
 * 
 */
public class TechnicalMetadataEnhancement extends Enhancement<Element> {
	Namespace ns = JDOMNamespaceUtil.FITS_NS;

	private static final Logger LOG = LoggerFactory.getLogger(TechnicalMetadataEnhancement.class);
	private static final int MAX_EXTENSION_LENGTH = 8;

	private TechnicalMetadataEnhancementService service = null;

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
			LOG.debug(this.getClass().getName() + " call method exited, service is not active.");
			return null;
		}

		// get sourceData data stream IDs
		List<String> srcDSURIs = this.service.getTripleStoreQueryService().getSourceData(pid);
		Map<String, String> sourceMimetype = new HashMap<String, String>(srcDSURIs.size());

		Map<String, Document> ds2FitsDoc = new HashMap<String, Document>();
		try {
			Document foxml = service.getManagementClient().getObjectXML(pid);

			for (String srcURI : srcDSURIs) { // for each source datastream
				LOG.debug("source data URI: " + srcURI);
				String dsid = srcURI.substring(srcURI.lastIndexOf("/") + 1);
				LOG.debug("datastream ID: " + dsid);

				// get current datastream version ID
				String dsLocation = null;
				String dsIrodsPath = null;
				String dsAltIds = null;
				Datastream ds = service.getManagementClient().getDatastream(pid, dsid, "");
				String vid = ds.getVersionID();
				Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, dsid);
				for (Object o : dsEl.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)) {
					if (o instanceof Element) {
						Element dsvEl = (Element) o;
						if (vid.equals(dsvEl.getAttributeValue("ID"))) {
							sourceMimetype.put(dsid, dsvEl.getAttributeValue("MIMETYPE"));
							dsLocation = dsvEl.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS)
									.getAttributeValue("REF");
							dsAltIds = dsvEl.getAttributeValue("ALT_IDS");
							break;
						}
					}
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
				String md5checksum = fits.getRootElement().getChild("fileinfo", FITS_NS)
						.getChildText("md5checksum", FITS_NS);
				String size = fits.getRootElement().getChild("fileinfo", FITS_NS).getChildText("size", FITS_NS);

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

				String fitsMimetype = null;
				String format = null;
				if (trustedIdentity != null) {
					fitsMimetype = trustedIdentity.getAttributeValue("mimetype");
					format = trustedIdentity.getAttributeValue("format");
				} else {
					format = "Unknown";
					LOG.warn("FITS unable to conclusively identify file: " + pid + "/" + dsid);
					LOG.info(new XMLOutputter().outputString(fits));
				}

				// If fedora has a meaningful mimetype already, then override the fits generate one.
				String fedoraMimetype = sourceMimetype.get(dsid);
				if (fedoraMimetype != null && fedoraMimetype.trim().length() > 0
						&& !fedoraMimetype.contains("octet-stream")) {
					fitsMimetype = fedoraMimetype;
				}

				if ("DATA_FILE".equals(dsid)) {
					if (fitsMimetype != null) {
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceMimeType.toString(),
								fitsMimetype, null);
					} else { // application/octet-stream
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceMimeType.toString(),
								"application/octet-stream", null);
					}

					try {
						Long.parseLong(size);
						setExclusiveTripleValue(pid, ContentModelHelper.CDRProperty.hasSourceFileSize.toString(), size,
								"http://www.w3.org/2001/XMLSchema#long");
					} catch (NumberFormatException e) {
						LOG.error("FITS produced a non-integer value for size: " + size);
					}
				}

				p.addContent(new Element("object", PREMIS_V2_NS)
						.addContent(
								new Element("objectIdentifier", PREMIS_V2_NS).addContent(
										new Element("objectIdentifierType", PREMIS_V2_NS).setText("Fedora Datastream PID"))
										.addContent(new Element("objectIdentifierValue", PREMIS_V2_NS).setText(dsid)))
						.addContent(
								new Element("objectCharacteristics", PREMIS_V2_NS)
										.addContent(new Element("compositionLevel", PREMIS_V2_NS).setText("0"))
										.addContent(
												new Element("fixity", PREMIS_V2_NS).addContent(
														new Element("messageDigestAlgorithm", PREMIS_V2_NS).setText("MD5"))
														.addContent(new Element("messageDigest", PREMIS_V2_NS).setText(md5checksum)))
										.addContent(new Element("size", PREMIS_V2_NS).setText(size))
										.addContent(
												new Element("format", PREMIS_V2_NS).addContent(new Element("formatDesignation",
														PREMIS_V2_NS).addContent(new Element("formatName", PREMIS_V2_NS)
														.setText(format))))
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

			LOG.debug("Finished MD_TECHNICAL updating for " + pid.getPid());
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
	 * Set a single value for a given predicate and pid.
	 * 
	 * @param pid
	 * @param predicate
	 * @param newExclusiveValue
	 * @throws FedoraException
	 */
	private void setExclusiveTripleValue(PID pid, String predicate, String newExclusiveValue, String datatype)
			throws FedoraException {
		List<String> rel = service.getTripleStoreQueryService().fetchAllTriples(pid).get(predicate);
		if (rel != null) {
			if (rel.contains(newExclusiveValue)) {
				rel.remove(newExclusiveValue);
			} else {
				// add missing rel
				service.getManagementClient().addLiteralStatement(pid, predicate, newExclusiveValue, datatype);
			}
			// remove any other same predicate triples
			for (String oldValue : rel) {
				service.getManagementClient().purgeLiteralStatement(pid, predicate, oldValue, datatype);
			}
		} else {
			// add missing rel
			service.getManagementClient().addLiteralStatement(pid, predicate, newExclusiveValue, datatype);
		}
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
					URI alt = new URI(altid);
					String rawPath = alt.getRawPath();
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
		LOG.debug("Run fits for " + dsIrodsPath);
		BufferedReader reader = null;
		String xmlstr = null;
		String errstr = null;
		try {
			if (filename == null) {
				reader = new BufferedReader(new InputStreamReader(service.remoteExecuteWithPhysicalLocation("fitsextract",
						dsIrodsPath)));
			} else {
				reader = new BufferedReader(new InputStreamReader(service.remoteExecuteWithPhysicalLocation("fitsextract",
						"'" + filename + "'", dsIrodsPath)));
			}
			StringBuilder xml = new StringBuilder();
			StringBuilder err = new StringBuilder();
			boolean blankReached = false;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.trim().length() == 0) {
					blankReached = true;
					continue;
				} else {
					if (blankReached) {
						err.append(line).append("\n");
					} else {
						xml.append(line).append("\n");
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

	public TechnicalMetadataEnhancement(TechnicalMetadataEnhancementService technicalMetadataEnhancementService, PID pid) {
		super(pid);
		this.service = technicalMetadataEnhancementService;
	}

}
