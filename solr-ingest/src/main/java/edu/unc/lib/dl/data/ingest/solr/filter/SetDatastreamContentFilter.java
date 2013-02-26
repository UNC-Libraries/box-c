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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.util.JDOMQueryUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

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

	private Pattern extensionRegex;
	private Properties mimetypeToExtensionMap;

	public SetDatastreamContentFilter() {
		try {
			extensionRegex = Pattern.compile("^[^\\n]*[^.]\\.(\\d*[a-zA-Z][a-zA-Z0-9]*)$");
			mimetypeToExtensionMap = new Properties();
			mimetypeToExtensionMap.load(new InputStreamReader(this.getClass().getResourceAsStream(
					"mimetypeToExtension.txt")));
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

		// Generate a defaultWebObject datastreams and add them to the list
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

		// Retrieve the default web data datastreams and use it to determine the content type for this object
		try {
			Datastream defaultWebData = this.getDefaultWebData(dip, datastreams);
			if (defaultWebData != null) {
				// Store the pid/datastream name of the default web datastream
				dip.setDefaultWebData(defaultWebData.getDatastreamIdentifier());

				// If the mimetype listed on the datastream is not very specific (octet-stream), see if FITS provided a
				// better one and use it instead
				if ("application/octet-stream".equals(defaultWebData.getMimetype())) {
					String sourceDataMimetype = relsExt.getChildText("hasSourceMimeType", JDOMNamespaceUtil.CDR_NS);
					if (sourceDataMimetype != null) {
						defaultWebData.setMimetype(sourceDataMimetype);
						// Use the extension if you've got it.  if not, get it
						if (defaultWebData.getExtension() == null)
							defaultWebData.setExtension(this.getExtension(null, defaultWebData.getMimetype()));
					}
				}
				
				// If the filesize on the datastream is not set (due to old version of fedora creating it), then grab it from rels-ext
				if (defaultWebData.getFilesize() < 0) {
					String sourceFileSize = relsExt.getChildText(ContentModelHelper.CDRProperty.hasSourceFileSize.name(), JDOMNamespaceUtil.CDR_NS);
					if (sourceFileSize != null) {
						defaultWebData.setFilesize(Long.parseLong(sourceFileSize));
					}
				}

				// Add in the content types for the dwd
				List<String> contentTypes = new ArrayList<String>();
				this.extractContentType(defaultWebData, contentTypes);
				dip.getDocument().setContentType(contentTypes);
				dip.getDocument().setFilesizeSort(defaultWebData.getFilesize());
			}
		} catch (JDOMException e) {
			throw new IndexingException("Failed to extract default web data for " + dip.getPid().getPid(), e);
		}
		
		long totalSize = 0;
		List<String> datastreamList = new ArrayList<String>(datastreams.size());
		dip.getDocument().setDatastream(datastreamList);
		for (Datastream ds : datastreams) {
			datastreamList.add(ds.toString());
			// Only add the filesize for datastreams directly belonging to this object to the filesize total
			if (ds.getOwner() == null)
				totalSize += ds.getFilesize();
		}
		dip.getDocument().setFilesizeTotal(totalSize);
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
		Datastream currentDS = null;
		long filesize = 0;

		Map<String, Element> datastreamMap = dip.getMostRecentDatastreamMap();
		Iterator<Entry<String, Element>> it = datastreamMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Element> dsEntry = it.next();
			String dsName = dsEntry.getKey();
			Element datastreamVersion = dsEntry.getValue();

			try {
				filesize = Long.parseLong(datastreamVersion.getAttributeValue("SIZE"));
			} catch (NumberFormatException numE) {
				filesize = 0;
			}

			currentDS = new Datastream(dsName);
			currentDS.setMimetype(datastreamVersion.getAttributeValue("MIMETYPE"));
			currentDS.setFilesize(filesize);
			currentDS.setExtension(this.getExtension(datastreamVersion.getAttributeValue("ALT_IDS"),
					currentDS.getMimetype()));
			if (includePIDAsOwner) {
				currentDS.setOwner(dip.getPid());
			}
			Element checksumEl = datastreamVersion.getChild("contentDigest", JDOMNamespaceUtil.FOXML_NS);
			if (checksumEl != null) {
				currentDS.setChecksum(checksumEl.getAttributeValue("DIGEST"));
			}
			datastreams.add(currentDS);
		}
	}

	private DocumentIndexingPackage getDefaultWebObject(Element relsExt) throws JDOMException {
		String defaultWebObject = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebObject.name(),
				JDOMNamespaceUtil.CDR_NS, relsExt);
		if (defaultWebObject == null)
			return null;

		PID dwoPID = new PID(defaultWebObject);
		log.debug("Retrieving default web object " + dwoPID.getPid());
		return dipFactory.createDocumentIndexingPackage(dwoPID);
	}

	// Used for determining sort file size, contentType. Store it for SetRelations
	private Datastream getDefaultWebData(DocumentIndexingPackage dip, List<Datastream> datastreams) throws JDOMException {
		PID owner = null;
		Element relsExt = dip.getRelsExt();
		String defaultWebData = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebData.name(),
				JDOMNamespaceUtil.CDR_NS, relsExt);

		// If this object does not have a defaultWebData but its defaultWebObject does, then use that instead.
		if (defaultWebData == null && dip.getDefaultWebObject() != null) {
			defaultWebData = JDOMQueryUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebData.name(),
					JDOMNamespaceUtil.CDR_NS, dip.getDefaultWebObject().getRelsExt());
			owner = dip.getDefaultWebObject().getPid();
		}
		if (defaultWebData == null)
			return null;

		// Find the datastream that matches the defaultWebData datastream name and owner.
		String dwdName = defaultWebData.substring(defaultWebData.lastIndexOf('/') + 1);
		for (Datastream ds : datastreams) {
			if (ds.getName().equals(dwdName) && (owner == ds.getOwner() || owner.equals(ds.getOwner())))
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
		ContentCategory contentCategory = getContentCategory(datastream.getMimetype(), datastream.getExtension());
		if (contentCategory == null)
			return;
		contentTypes.add('^' + contentCategory.joined);
		StringBuilder contentType = new StringBuilder();
		contentType.append('/').append(contentCategory.key).append('^').append(datastream.getExtension()).append(',')
				.append(datastream.getExtension());
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

		if ("exe".equals(extension) || "app".equals(extension))
			return SOFTWARE;

		if ("application/vnd.ms-excel".equals(mimetype) || "csv".equals(extension) || "mdb".equals(extension)
				|| "xlsx".equals(extension) || "xls".equals(extension) || "log".equals(extension) || "db".equals(extension)
				|| "accdb".equals(extension))
			return DATASET;

		if (mimetypeType.equals("text"))
			return TEXT;
		if ("application/msword".equals(mimetype) || "application/rtf".equals(mimetype)
				|| "application/pdf".equals(mimetype) || "application/postscript".equals(mimetype)
				|| "application/powerpoint".equals(mimetype) || "application/vnd.ms-powerpoint".equals(mimetype)
				|| "pdf".equals(extension) || "doc".equals(extension) || "docx".equals(extension)
				|| "dotx".equals(extension) || "ppt".equals(extension) || "pptx".equals(extension)
				|| "wp".equals(extension) || "wpd".equals(extension) || "xml".equals(extension) || "htm".equals(extension)
				|| "html".equals(extension) || "shtml".equals(extension) || "php".equals(extension)
				|| "js".equals(extension))
			return TEXT;

		if ("application/ogg".equals(mimetype) || "mp3".equals(extension) || "wav".equals(extension)
				|| "rm".equals(extension))
			return AUDIO;

		if ("application/jpg".equals(mimetype) || "jpg".equals(extension) || "psd".equals(extension)
				|| "psf".equals(extension) || "pct".equals(extension) || "ttf".equals(extension)
				|| "jpeg".equals(extension) || "indd".equals(extension))
			return IMAGE;

		if ("mp4".equals(extension) || "m4v".equals(extension) || "mpg".equals(extension) || "swf".equals(extension)
				|| "application/x-shockwave-flash".equals(mimetype) || "application/x-silverlight-app".equals(mimetype))
			return VIDEO;

		if ("application/x-gtar".equals(mimetype) || "application/x-gzip".equals(mimetype)
				|| "application/x-compress".equals(mimetype) || "application/x-compressed".equals(mimetype)
				|| "application/zip".equals(mimetype) || "application/x-stuffit".equals(mimetype)
				|| "application/x-tar".equals(mimetype) || "gz".equals(extension) || "zip".equals(extension))
			return ARCHIVE;

		if ("dmg".equals(extension) || "iso".equals(extension) || "disk".equals(extension))
			return DISK_IMAGE;

		return UNKNOWN;
	}

	private static class ContentCategory {
		String key;
		String joined;

		public ContentCategory(String key, String label) {
			this.key = key;
			this.joined = key + "," + label;
		}
	}
}
