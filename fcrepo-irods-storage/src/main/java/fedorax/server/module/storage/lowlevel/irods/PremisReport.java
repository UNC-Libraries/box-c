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
package fedorax.server.module.storage.lowlevel.irods;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.Access;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DigitalObject;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.query.IRODSQueryResultRow;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This report builder maps Fedora objects onto the PREMIS object schema with relevant storage metadata from iRODS. It
 * is designed as part of the Fedora32-iRODS low level storage module and therefore only supplies information which is
 * general to the Fedora object model or iRODS system metadata. Output from this report builder may be merged with other
 * PREMIS sources to create more comprehensive reports. This report includes object identifier types that are Fedora
 * object PIDs and the less familiar Fedora datastream version PIDs. These PIDs can be used to merge reports and
 * navigate the object relationships within them.
 *
 * @see http://www.loc.gov/standards/premis/
 * @author Gregory Jansen
 *
 */
public class PremisReport {

	private static final Logger LOG = LoggerFactory.getLogger(PremisReport.class);
	private final String premisUri = "info:lc/xmlns/premis-v2";
	private final Namespace p = Namespace.getNamespace(premisUri);

	private final String xsiUri = "http://www.w3c.org/2001/XMLSchema-instance";
	private final Namespace xsi = Namespace.getNamespace("xsi", xsiUri);

	/** Instance of the Fedora Server */
	private Context context = null;
	private Access access = null;
	private DOManager manager = null;

	/** Instance of the low-level storage subsystem */
	private IrodsLowlevelStorageModule irodslls = null;

	public PremisReport(Server fcserver, Context context) throws Exception {
		this.context = context;

		access = (Access) fcserver.getModule("org.fcrepo.server.access.Access");
		if (access == null) {
			throw new Exception("[PremisReport] Can't get a ref to Access from Server.getModule");
		}

		manager = (DOManager) fcserver.getModule("org.fcrepo.server.storage.DOManager");
		if (manager == null) {
			throw new Exception("Can't get a DOManager " + "from Server.getModule");
		}

		Module m = fcserver.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
		if (m instanceof IrodsLowlevelStorageModule) {
			irodslls = (IrodsLowlevelStorageModule) m;
		} else {
			throw new Exception("Error, unsupported low-level storage module for Storage Reports");
		}
	}

	public Document getXMLReport(String pid) throws ServerException {
		Document result = new Document();
		Element premis = new Element("premis", p);
		premis.addNamespaceDeclaration(xsi);
		premis.addNamespaceDeclaration(p);
		premis.setAttribute("version", "2.0");
		result.addContent(premis);

		if (!manager.objectExists(pid)) {
			throw new ObjectNotFoundException("No such PID is registered in this Fedora instance.");
		}
		DOReader r = manager.getReader(Server.GLOBAL_CHOICE, context, pid);
		DigitalObject object = r.getObject();

		// report the datastreams

		// after datastream processing this will hold the current version date
		// for all datastreams
		HashMap<String, String> dsid2CurrVID = new HashMap<String, String>();

		Iterator<String> dsids = object.datastreamIdIterator();
		while (dsids.hasNext()) {
			String dsid = dsids.next();
			String prevVID = null;
			String nextVID = null;

			// first gather dates for all versions of this datastream
			SortedMap<Date, String> vDates2ID = new TreeMap<Date, String>();
			for (Datastream ds : object.datastreams(dsid)) {
				vDates2ID.put(ds.DSCreateDT, ds.DSVersionID);
			}
			dsid2CurrVID.put(dsid, vDates2ID.get(vDates2ID.lastKey()));

			LinkedList<String> versions = new LinkedList<String>();
			versions.addAll(vDates2ID.values()); // VIDs in ascending date order

			for (Datastream ds : object.datastreams(dsid)) {
				// get the previous and next version ids
				int i = versions.indexOf(ds.DSVersionID);
				if (i - 1 >= 0) {
					prevVID = versions.get(i - 1);
				}
				if (i + 1 < versions.size()) {
					nextVID = versions.get(i + 1);
				}

				if ("M".equals(ds.DSControlGrp)) {
					reportManagedDatastreamFile(premis, pid, ds, prevVID, nextVID);
				} else if ("X".equals(ds.DSControlGrp)) {
					reportInlineXMLBitstream(premis, pid, ds, prevVID, nextVID);
				}
			}
		}

		// report object-level
		reportFedoraObjectRepresentation(premis, object, dsid2CurrVID);
		return result;
	}

	private void reportFedoraObjectRepresentation(Element premis, DigitalObject obj, HashMap<String, String> dsid2CurrVID) {
		Element object = new Element("object", p);
		object.setAttribute("type", "representation", xsi);
		object.addContent(new Element("objectIdentifier", p).addContent(
				new Element("objectIdentifierType", p).setText("Fedora PID")).addContent(
				new Element("objectIdentifierValue", p).setText(obj.getPid())));
		object.addContent(new Element("originalName", p).setText(obj.getLabel()));

		// these point to the current version ids
		Iterator<String> dsids = obj.datastreamIdIterator();
		while (dsids.hasNext()) {
			String dsid = dsids.next();
			String dsVID = obj.getPid() + "+" + dsid + "+" + dsid2CurrVID.get(dsid);
			object.addContent(new Element("relationship", p)
					.addContent(new Element("relationshipType", p).setText("has part"))
					.addContent(new Element("relationshipSubType", p).setText("fedora-system:hasDatastream"))
					.addContent(
							new Element("relatedObjectIdentification", p).addContent(
									new Element("relatedObjectIdentifierType", p).setText("Fedora Datastream PID")).addContent(
									new Element("relatedObjectIdentifierValue", p).setText(dsVID))));
		}
		// for visibility the main representation goes at the top of the report
		premis.addContent(0, object);
	}

	private void reportInlineXMLBitstream(Element premis, String pid, Datastream ds, String prevVID, String nextVID) {
		Element object = new Element("object", p);
		object.setAttribute("type", "bitstream", xsi);
		String dsPID = pid + "+" + ds.DatastreamID + "+" + ds.DSVersionID;
		long size = ds.DSSize;
		object.addContent(
				new Element("objectIdentifier", p).addContent(
						new Element("objectIdentifierType", p).setText("Fedora Datastream PID")).addContent(
						new Element("objectIdentifierValue", p).setText(dsPID))).addContent(
				new Element("objectCharacteristics", p)
						.addContent(new Element("compositionLevel", p).setText("1"))
						.addContent(new Element("size", p).setText(Integer.toString((int) size)))
						.addContent(
								new Element("format", p).addContent(
										new Element("formatDesignation", p).addContent(new Element("formatName", p)
												.setText(ds.DSMIME))).addContent(
										new Element("formatNote", p).setText("IANA MIME-type"))));
		IRODSQueryResultSet info = null;
		try {
			info = irodslls.getFOXMLStorageMetadata(pid);
			for (IRODSQueryResultRow row : info.getResults()) {
				reportStorageLocation(object, row);
			}
		} catch (LowlevelStorageException e) {
			throw new Error("Could not get iRODS storage information for " + dsPID, e);
		}

		// output rels to prev and next versions
		if (prevVID != null) {
			String prevDSVID = pid + "+" + ds.DatastreamID + "+" + prevVID;
			object.addContent(new Element("relationship", p)
					.addContent(new Element("relationshipType", p).setText("has previous version"))
					.addContent(new Element("relatedObjectIdentification", p))
					.addContent(new Element("relatedObjectIdentifierType", p).setText("Fedora Datastream PID"))
					.addContent(new Element("relatedObjectIdentifierValue", p).setText(prevDSVID)));
		}
		if (nextVID != null) {
			String nextDSVID = pid + "+" + ds.DatastreamID + "+" + nextVID;
			object.addContent(new Element("relationship", p)
					.addContent(new Element("relationshipType", p).setText("has next version"))
					.addContent(new Element("relatedObjectIdentification", p))
					.addContent(new Element("relatedObjectIdentifierType", p).setText("Fedora Datastream PID"))
					.addContent(new Element("relatedObjectIdentifierValue", p).setText(nextDSVID)));
		}
		premis.addContent(object);
	}

	private void reportManagedDatastreamFile(Element premis, String pid, Datastream ds, String prevVID, String nextVID) {
		String dsPID = pid + "+" + ds.DatastreamID + "+" + ds.DSVersionID;
		LOG.info("reportManagedDatastream(): " + dsPID);
		String digestAlgorithm = "MD5";
		String digestOrigin = "IRODS Microservice";
		String formatNote = "unvalidated IANA MIME-type";
		String digest = null;
		String size = null;
		IRODSQueryResultSet info = null;
		try {
			info = irodslls.getDatastreamStorageMetadata(dsPID);
			IRODSQueryResultRow r = info.getFirstResult();
			size = r.getColumn(RodsGenQueryEnum.COL_DATA_SIZE.getName());
			digest = r.getColumn(RodsGenQueryEnum.COL_D_DATA_CHECKSUM.getName());
		} catch (LowlevelStorageException e) {
			throw new Error("Could not get iRODS storage information for " + dsPID, e);
		} catch (JargonException e) {
			throw new Error("Could not get iRODS storage information for " + dsPID, e);
		}
		Element object = new Element("object", p);
		object.setAttribute("type", "file", xsi);
		object.addContent(
				new Element("objectIdentifier", p).addContent(
						new Element("objectIdentifierType", p).setText("Fedora Datastream PID")).addContent(
						new Element("objectIdentifierValue", p).setText(dsPID)))
				.addContent(
						new Element("objectCharacteristics", p)
								.addContent(new Element("compositionLevel", p).setText("0"))
								.addContent(
										new Element("fixity", p)
												.addContent(new Element("messageDigestAlgorithm", p).setText(digestAlgorithm))
												.addContent(new Element("messageDigest", p).setText(digest))
												.addContent(new Element("messageDigestOriginator", p).setText(digestOrigin)))
								.addContent(new Element("size", p).setText(size))
								.addContent(
										new Element("format", p).addContent(
												new Element("formatDesignation", p).addContent(new Element("formatName", p)
														.setText(ds.DSMIME))).addContent(
												new Element("formatNote", p).setText(formatNote))));
		if (ds.DatastreamAltIDs.length > 0) {
			object.addContent(new Element("originalName", p).setText(ds.DatastreamAltIDs[0]));
		} else {
			object.addContent(new Element("originalName", p).setText(ds.DSLabel));
		}

		// add storage nodes
		for (IRODSQueryResultRow r : info.getResults()) {
			reportStorageLocation(object, r);
		}

		// output rels to prev and next versions
		if (prevVID != null) {
			String prevDSVID = pid + "+" + ds.DatastreamID + "+" + prevVID;
			object.addContent(new Element("relationship", p)
					.addContent(new Element("relationshipType", p).setText("has previous version"))
					.addContent(new Element("relatedObjectIdentification", p))
					.addContent(new Element("relatedObjectIdentifierType", p).setText("Fedora Datastream PID"))
					.addContent(new Element("relatedObjectIdentifierValue", p).setText(prevDSVID)));
		}
		if (nextVID != null) {
			String nextDSVID = pid + "+" + ds.DatastreamID + "+" + nextVID;
			object.addContent(new Element("relationship", p)
					.addContent(new Element("relationshipType", p).setText("has next version"))
					.addContent(new Element("relatedObjectIdentification", p))
					.addContent(new Element("relatedObjectIdentifierType", p).setText("Fedora Datastream PID"))
					.addContent(new Element("relatedObjectIdentifierValue", p).setText(nextDSVID)));
		}
		premis.addContent(object);
	}

	// file size: 609960
	// file modification date: 1244062144
	// file creation date: 01241102081
	// file owner: fedora
	// File Checksum: 10be9cd0e98712a4ecad507920db5199
	// File Version:
	// file name: BigFOXML.xml
	// directory name: /cdrZone/home/fedora
	// File Replica Number: 0
	// File Replica Status: 1
	// File Resource Group Name:
	// File Map Identifier: 0
	// File Expiration time:
	// File Data Status:
	// File Type: generic
	// Resource Name: cdrResc
	// Resource Location: ono-sendai.dhcp.unc.edu
	// Resource Class: archive
	// Resource Comment:
	// Resource Information:
	// Resource Type: unix file system
	// Resource Vault Path: /opt/iRODS/Vault
	// Resource Zone Name: cdrZone

	private void reportStorageLocation(Element object, IRODSQueryResultRow info) {
		try {
			//String replicaNum = info.getColumn(RodsGenQueryEnum.COL_DATA_REPL_NUM.getName());
			String port = String.valueOf(this.irodslls.getAccount().getPort());
			String rescLocation = info.getColumn(RodsGenQueryEnum.COL_R_LOC.getName());
			String dir = info.getColumn(RodsGenQueryEnum.COL_COLL_NAME.getName());
			String filename = info.getColumn(RodsGenQueryEnum.COL_DATA_NAME.getName());
			String rescType = info.getColumn(RodsGenQueryEnum.COL_R_TYPE_NAME.getName());
			String rescClass = info.getColumn(RodsGenQueryEnum.COL_R_CLASS_NAME.getName());
			// example URI irods://myUser:myPassword@myirodshost.org:1247/myDirectory/myFile
			String contentLocURI = "irods://" + rescLocation + ":" + port + dir + "/" + filename;
			String storageMedium = rescClass + " - " + rescType;
			object.addContent(new Element("storage", p).addContent(
					new Element("contentLocation", p).addContent(new Element("contentLocationType", p).setText("iRODS URI"))
							.addContent(new Element("contentLocationValue", p).setText(contentLocURI))).addContent(
					new Element("storageMedium", p).setText(storageMedium)));
		} catch (JargonException e) {
			throw new Error(e);
		}
	}

	// private static String getMeta(MetaDataRecordList md, String fieldName) {
	// return md.getStringValue(md.getFieldIndex(fieldName));
	// }
}
