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
package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * This filter checks the integrity of files for ingest. It will throw an IngestFilterException under these conditions:
 * - a file referenced in FOXML is not resolvable in a supported way - a supplied checksum doesn't match one locally
 * computed - there are extra files in the SIP
 *
 * @author count0
 */
public class METSPackageFileValidator {
	private static XPath fileFinderXpath;
	private static XPath allFilesXpath;
	private static final Namespace _METSNamespace = Namespace.getNamespace("m", JDOMNamespaceUtil.METS_NS.getURI());
	private static final String fileFinderXpathStr = "//m:file[m:FLocat/@xlink:href = $locator]";
	private static final String allFilesXpathStr = "//m:file";
	private static final Log log = LogFactory.getLog(METSPackageFileValidator.class);
	static {
		try {
			fileFinderXpath = XPath.newInstance(fileFinderXpathStr);
			fileFinderXpath.addNamespace(_METSNamespace);
			allFilesXpath = XPath.newInstance(allFilesXpathStr);
			allFilesXpath.addNamespace(_METSNamespace);
		} catch (JDOMException e) {
			throw new java.lang.ExceptionInInitializerError(e);
		}
	}

	/**
	 * Checks that there are as many files packaged as there are non-staged file references. Computes and compares the
	 * MD5 digest of packaged files that have a checksum in METS. Checks access to all files referenced in staging
	 * locations.
	 *
	 * @param mets
	 * @param metsPack
	 * @param aip
	 * @throws IngestException
	 */
	@SuppressWarnings("unchecked")
	public void validateFiles(Document mets, METSPackageSIP metsPack)
			throws IngestException {
		StringBuffer errors = new StringBuffer();
		List<File> manifestFiles = new ArrayList<File>();
		List<String> missingFiles = new ArrayList<String>();
		List<String> badChecksumFiles = new ArrayList<String>();

		// find missing or corrupt files listed in manifest
		try {
			for(Element fileEl : (List<Element>)allFilesXpath.selectNodes(mets)) {
				String href = null;
				try {
					href = fileEl.getChild("FLocat", JDOMNamespaceUtil.METS_NS).getAttributeValue("href", JDOMNamespaceUtil.XLINK_NS);
					URI uri = new URI(href);
					if (uri.getScheme() != null && !uri.getScheme().contains("file")) {
						continue;
					}
				} catch (URISyntaxException e) {
					errors.append("Cannot parse file location: " + e.getLocalizedMessage() + " (" + href + ")");
					missingFiles.add(href);
					continue;
				} catch (NullPointerException e) {
					errors.append("A file location is missing for file ID: " + fileEl.getAttributeValue("ID"));
					continue;					
				}
				File file = null;
				// locate the file and check that it exists
				try {
					log.debug("Looking in SIP");
					file = metsPack.getFileForLocator(href);
					file.equals(file);
					manifestFiles.add(file);
					log.debug("FILE IS IN METSPackage: " + file.getPath());
					if (file == null || !file.exists()) {
						missingFiles.add(href);
						continue;
					}
				} catch (IOException e) {
					log.debug(e);
					missingFiles.add(href);
					errors.append(e.getMessage());
				}

				String checksum = fileEl.getAttributeValue("CHECKSUM");
				if (checksum != null) {
					log.debug("found a checksum in METS");
					Checksum checker = new Checksum();
					try {
						String sum = checker.getChecksum(file);
						if (!sum.equals(checksum.toLowerCase())) {
							log.debug("Checksum failed for file: " + href + " (METS says " + checksum + ", but we got " + sum
									+ ")");
							badChecksumFiles.add(href);
						}
						log.debug("METS manifest checksum was verified for file: " + href);
					} catch (IOException e) {
						throw new IngestException("Checksum failed to find file: " + href);
					}
				}
			}
		} catch (JDOMException e1) {
			throw new Error("Unexpected JDOM Exception", e1);
		}

		// TODO: account for local (not inline xmlData) MODS files
		// see if there are extra files in the SIP
		List<String> extraFiles = new ArrayList<String>();

		if (metsPack.getSIPDataDir() != null) {
			int zipPathLength = 0;
			try {
				zipPathLength = metsPack.getSIPDataDir().getCanonicalPath().length();

				for (File received : metsPack.getDataFiles()) {
					if (!manifestFiles.contains(received) && received.compareTo(metsPack.getMetsFile()) != 0) {
						extraFiles.add("file://" + received.getCanonicalPath().substring(zipPathLength));
					}
				}
			} catch (IOException e) {
				throw new Error("Unexpected IO Exception trying to get path of a known file.", e);
			}
		}
		if (missingFiles.size() > 0 || badChecksumFiles.size() > 0 || extraFiles.size() > 0) {
			// We have an error here...
			String msg = "The files submitted do not match those listed in the METS manifest.";
			FilesDoNotMatchManifestException e = new FilesDoNotMatchManifestException(msg);
			e.setBadChecksumFiles(badChecksumFiles);
			e.setExtraFiles(extraFiles);
			e.setMissingFiles(missingFiles);
			throw e;
		}
	}
}
