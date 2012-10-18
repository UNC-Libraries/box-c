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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.util.JDOMQueryUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.NamespaceConstants;

/**
 * Extracts datastreams from an object and sets related properties concerning the default datastream for the object,
 * including the mimetype and extension into the content type hierarchical facet.
 * 
 * Sets datastream, contentType, filesizeTotal, filesizeSort
 * 
 * @author bbpennel
 * 
 */
public class SetDatastreamContentFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetDatastreamContentFilter.class);

	private final ContentCategory DATASET = new ContentCategory("dataset", "Dataset");
	private final ContentCategory IMAGE = new ContentCategory("image", "Image");
	private final ContentCategory DISK_IMAGE = new ContentCategory("diskimage", "Disk Image");
	private final ContentCategory VIDEO = new ContentCategory("video", "Video");
	private final ContentCategory SOFTWARE = new ContentCategory("software", "Software");
	private final ContentCategory AUDIO = new ContentCategory("audio", "Audio");
	private final ContentCategory ARCHIVE = new ContentCategory("archive", "Archive File");
	private final ContentCategory TEXT = new ContentCategory("text", "Text");
	private final ContentCategory UNKNOWN = new ContentCategory("unknown", "Unknown");

	private XPath datastreamVersionXPath;
	private Pattern extensionRegex;
	private Properties mimetypeToExtensionMap;

	private DocumentIndexingPackageFactory dipFactory;

	public SetDatastreamContentFilter() {
		try {
			datastreamVersionXPath = XPath.newInstance("/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion");
			datastreamVersionXPath.addNamespace(Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI));
			extensionRegex = Pattern.compile("^[^\\n]*[^.]\\.(\\d*[a-zA-Z][a-zA-Z0-9]*)$");
			mimetypeToExtensionMap = new Properties();
			mimetypeToExtensionMap.load(new InputStreamReader(this.getClass().getResourceAsStream(
					"mimetypeToExtension.txt")));

		} catch (JDOMException e) {
			log.error("Failed to initialize queries", e);
		} catch (FileNotFoundException e) {
			log.error("Failed to load mimetype mappings", e);
		} catch (IOException e) {
			log.error("Failed to load mimetype mappings", e);
		}
	}

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Document foxml = dip.getFoxml();
		if (foxml == null)
			throw new IndexingException("FOXML was not found or set for " + dip.getPid());

		// Generate list of datastreams on this object
		List<Datastream> datastreams = new ArrayList<Datastream>();
		this.extractDatastreams(dip, datastreams, false);

		Element relsExt = dip.getRelsExt();

		// Generatea defaultWebObject datastreams and add them to the list
		try {
			DocumentIndexingPackage dwoDIP = this.getDefaultWebObject(relsExt);
			dip.setAttemptedToRetrieveDefaultWebObject(true);
			if (dwoDIP != null) {
				dip.setDefaultWebObject(dwoDIP);
				this.extractDatastreams(dwoDIP, datastreams, true);
			}
		} catch (JDOMException e) {
			throw new IndexingException("Failed to parse document for default web object of " + dip.getPid().getPid(), e);
		}

		long totalSize = 0;
		List<String> datastreamList = new ArrayList<String>(datastreams.size());
		dip.getDocument().setDatastream(datastreamList);
		for (Datastream ds : datastreams) {
			datastreamList.add(ds.toString());
			// Only add the filesize for datastreams directly belonging to this object to the filesize total
			if (ds.ownerPID == null)
				totalSize += ds.filesize;
		}
		dip.getDocument().setFilesizeTotal(totalSize);

		// Retrieve the default web data datastreams and use it to determine the content type for this object
		try {
			Datastream defaultWebData = this.getDefaultWebData(dip, datastreams);
			if (defaultWebData != null) {
				// Store the pid/datastream name of the default web datastream
				dip.setDefaultWebData(defaultWebData.getDatastreamIdentifier());

				// If the mimetype listed on the datastream is not very specific (octet-stream), see if FITS provided a
				// better one and use it instead
				if ("application/octet-stream".equals(defaultWebData.mimetype)) {
					String sourceDataMimetype = relsExt.getChildText("hasSourceMimeType", JDOMNamespaceUtil.CDR_NS);
					if (sourceDataMimetype != null) {
						defaultWebData.mimetype = sourceDataMimetype;
						defaultWebData.extension = this.getExtension(null,
								defaultWebData.mimetype);
					}
				}

				// Add in the content types for the dwd
				List<String> contentTypes = new ArrayList<String>();
				this.extractContentType(defaultWebData, contentTypes);
				dip.getDocument().setContentType(contentTypes);
				dip.getDocument().setFilesizeSort(defaultWebData.filesize);
			}
		} catch (JDOMException e) {
			throw new IndexingException("Failed to extract default web data for " + dip.getPid().getPid(), e);
		}

	}

	/**
	 * Extracts a list of datastreams from a FOXML documents, including the datastream's name, file type, file size and
	 * backing enumeration.
	 * 
	 * @param dip
	 * @param datastreams
	 *           List of datastreams to add to
	 * @param includePIDAsOwner
	 *           If true, then the PID of the provided DIP will be listed as the owner of the datastream
	 */
	private void extractDatastreams(DocumentIndexingPackage dip, List<Datastream> datastreams, boolean includePIDAsOwner) {
		try {
			Document foxml = dip.getFoxml();

			boolean newDS = true;
			Datastream currentDS = null;
			String mostRecentDate = null;
			String createdDate;
			long filesize = 0;

			List<?> datastreamVersions = this.datastreamVersionXPath.selectNodes(foxml);
			for (Object datastreamVersionObj : datastreamVersions) {
				Element datastreamVersion = (Element) datastreamVersionObj;
				String dsID = datastreamVersion.getAttributeValue("ID");
				int index = dsID.lastIndexOf(".");
				String dsName = dsID.substring(0, index);

				createdDate = datastreamVersion.getAttributeValue("CREATED");
				try {
					filesize = Long.parseLong(datastreamVersion.getAttributeValue("SIZE"));
				} catch (NumberFormatException numE) {
					filesize = 0;
				}

				newDS = currentDS == null || !currentDS.dsName.equals(dsName);
				if (newDS) {
					currentDS = new Datastream(dsName);
					datastreams.add(currentDS);
				}

				if (newDS || createdDate.compareTo(mostRecentDate) > 0) {
					currentDS.mimetype = datastreamVersion.getAttributeValue("MIMETYPE");
					currentDS.filesize = filesize;
					currentDS.dsEnum = ContentModelHelper.Datastream.getDatastream(dsName);
					currentDS.extension = this.getExtension(datastreamVersion.getAttributeValue("ALT_IDS"),
							currentDS.mimetype);
					if (includePIDAsOwner) {
						currentDS.ownerPID = dip.getPid().getPid();
					}
					mostRecentDate = createdDate;
				}
			}
		} catch (JDOMException e) {
			throw new IndexingException("Could not extract datastream versions for " + dip.getPid());
		}
	}

	private DocumentIndexingPackage getDefaultWebObject(Element relsExt) throws JDOMException {
		String defaultWebObject = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebObject.name(),
				JDOMNamespaceUtil.CDR_NS, relsExt);
		if (defaultWebObject == null)
			return null;

		PID dwoPID = new PID(defaultWebObject);
		return dipFactory.createDocumentIndexingPackage(dwoPID);
	}

	// Used for determining sort file size, contentType. Store it for SetRelations
	private Datastream getDefaultWebData(DocumentIndexingPackage dip, List<Datastream> datastreams) throws JDOMException {
		String owner = null;
		Element relsExt = dip.getRelsExt();
		String defaultWebData = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebData.name(),
				JDOMNamespaceUtil.CDR_NS, relsExt);

		// If this object does not have a defaultWebData but its defaultWebObject does, then use that instead.
		if (defaultWebData == null && dip.getDefaultWebObject() != null) {
			defaultWebData = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebData.name(),
					JDOMNamespaceUtil.CDR_NS, dip.getDefaultWebObject().getRelsExt());
			owner = dip.getDefaultWebObject().getPid().getPid();
		}
		if (defaultWebData == null)
			return null;

		// Find the datastream that matches the defaultWebData datastream name and owner.
		String dwdName = defaultWebData.substring(defaultWebData.lastIndexOf('/') + 1);
		for (Datastream ds : datastreams) {
			if (ds.dsName.equals(dwdName) && (owner == ds.ownerPID || owner.equals(ds.ownerPID)))
				return ds;
		}

		return null;
	}

	private String getExtension(String filepath, String mimetype) {
		if (filepath != null) {
			Matcher matcher = extensionRegex.matcher(filepath);
			if (matcher.matches()) {
				String extension = matcher.group(1);
				if (extension.length() <= 8)
					return extension.toLowerCase();
			}
		}
		if (mimetype != null) {
			String extension = this.mimetypeToExtensionMap.getProperty(mimetype);
			return extension;
		}
		return null;
	}

	private void extractContentType(Datastream datastream, List<String> contentTypes) {
		ContentCategory contentCategory = getContentCategory(datastream.mimetype, datastream.extension);
		if (contentCategory == null)
			return;
		contentTypes.add('|' + contentCategory.joined);
		StringBuilder contentType = new StringBuilder();
		contentType.append('/').append(contentCategory.key).append('|').append(datastream.extension).append(',')
				.append(datastream.extension);
		contentTypes.add(contentType.toString());
	}

	private ContentCategory getContentCategory(String mimetype, String extension) {
		int index = mimetype.indexOf('/');
		String mimetypeType = mimetype.substring(0, index);
		if (mimetypeType.equals("image"))
			return IMAGE;
		if (mimetypeType.equals("video"))
			return VIDEO;
		if (mimetypeType.equals("audio"))
			return AUDIO;

		if (extension.equals("exe") || extension.equals("app"))
			return SOFTWARE;

		if (mimetype.equals("application/vnd.ms-excel") || extension.equals("csv") || extension.equals("mdb")
				|| extension.equals("xlsx") || extension.equals("xls") || extension.equals("log") || extension.equals("db")
				|| extension.equals("accdb"))
			return DATASET;

		if (mimetypeType.equals("text"))
			return TEXT;
		if (mimetype.equals("application/msword") || mimetype.equals("application/rtf")
				|| mimetype.equals("application/pdf") || mimetype.equals("application/postscript")
				|| mimetype.equals("application/powerpoint") || mimetype.equals("application/vnd.ms-powerpoint")
				|| extension.equals("pdf") || extension.equals("doc") || extension.equals("docx")
				|| extension.equals("dotx") || extension.equals("ppt") || extension.equals("pptx")
				|| extension.equals("wp") || extension.equals("wpd") || extension.equals("xml") || extension.equals("htm")
				|| extension.equals("html") || extension.equals("shtml") || extension.equals("php")
				|| extension.equals("js"))
			return TEXT;

		if (mimetype.equals("application/ogg") || extension.equals("mp3") || extension.equals("wav")
				|| extension.equals("rm"))
			return AUDIO;

		if (mimetype.equals("application/jpg") || extension.equals("jpg") || extension.equals("psd")
				|| extension.equals("psf") || extension.equals("pct") || extension.equals("ttf")
				|| extension.equals("jpeg") || extension.equals("indd"))
			return IMAGE;

		if (extension.equals("mp4") || extension.equals("m4v") || extension.equals("mpg") || extension.equals("swf")
				|| mimetype.equals("application/x-shockwave-flash") || mimetype.equals("application/x-silverlight-app"))
			return VIDEO;

		if (mimetype.equals("application/x-gtar") || mimetype.equals("application/x-gzip")
				|| mimetype.equals("application/x-compress") || mimetype.equals("application/x-compressed")
				|| mimetype.equals("application/zip") || mimetype.equals("application/x-stuffit")
				|| mimetype.equals("application/x-tar") || extension.equals("gz") || extension.equals("zip"))
			return ARCHIVE;

		if (extension.equals("dmg") || extension.equals("iso") || extension.equals("disk"))
			return DISK_IMAGE;

		return UNKNOWN;
	}

	public void setDipFactory(DocumentIndexingPackageFactory dipFactory) {
		this.dipFactory = dipFactory;
	}

	private static class ContentCategory {
		String key;
		String joined;

		public ContentCategory(String key, String label) {
			this.key = key;
			this.joined = key + "," + label;
		}
	}

	private static class Datastream {
		ContentModelHelper.Datastream dsEnum;
		String dsName;
		long filesize;
		String mimetype;
		String extension;
		String ownerPID;

		public Datastream(String dsName) {
			this.dsName = dsName;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(dsName);
			sb.append('|').append(filesize).append('|').append(mimetype).append('|');
			if (extension != null)
				sb.append(extension);
			sb.append('|');
			if (dsEnum != null)
				sb.append(dsEnum.getCategory().name());
			sb.append('|');
			if (ownerPID != null)
				sb.append(ownerPID);
			return sb.toString();
		}

		public String getDatastreamIdentifier() {
			if (ownerPID == null)
				return dsName;
			return ownerPID + "/" + dsName;
		}
	}

}
