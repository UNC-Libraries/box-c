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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * This filter checks the integrity of files for ingest. It will throw an IngestFilterException under these conditions:
 * - a file referenced in FOXML is not resolvable in a supported way - a supplied checksum doesn't match one locally
 * computed - there are extra files in the SIP
 *
 * @author count0
 */
public class METSPackageFileValidator {
	private static XPath _filesXpath;
	private static final Namespace _METSNamespace = Namespace.getNamespace("m", JDOMNamespaceUtil.METS_NS.getURI());
	private static final String filesXpath = "/m:mets/m:fileSec/m:fileGrp/m:file[m:FLocat/@xlink:href = $locator]";
	private static final Log log = LogFactory.getLog(METSPackageFileValidator.class);
	static {
		try {
			_filesXpath = XPath.newInstance(filesXpath);
			_filesXpath.addNamespace(_METSNamespace);
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
	public void validateFiles(Document mets, METSPackageSIP metsPack, ArchivalInformationPackage aip)
			throws IngestException {
		StringBuffer errors = new StringBuffer();
		int fileCount = 0;
		List<File> manifestFiles = new ArrayList<File>();
		List<String> missingFiles = new ArrayList<String>();
		List<String> badChecksumFiles = new ArrayList<String>();

		// find missing or corrupt files listed in manifest
		for (PID pid : aip.getPIDs()) {
			Document foxml = aip.getFOXMLDocument(pid);
			for (Element contentLocation : FOXMLJDOMUtil.getFileLocators(foxml)) {
				String url = contentLocation.getAttributeValue("REF");
				// log.debug("PROCESSING FILE LOCATION: " + url);
				// FIXME: this should whitelist "file:" or no scheme instead
				try {
					URI uri = new URI(url);
					if (uri.getScheme() != null && !uri.getScheme().contains("file")) {
						continue;
					}
				} catch(URISyntaxException e) {
					errors.append("Cannot parse file location: "+e.getLocalizedMessage()+" ("+url+")");
					missingFiles.add(url);
					continue;
				}
				fileCount++;
				File file = null;
				// locate the file and check that it exists
				try {
					log.debug("Looking in SIP");
					file = metsPack.getFileForLocator(url);
					file.equals(file);
					manifestFiles.add(file);
					log.debug("FILE IS IN METSPackage: " + file.getPath());
					if (file == null || !file.exists()) {
						missingFiles.add(url);
						continue;
					}
				} catch (IOException e) {
					log.debug(e);
					missingFiles.add(url);
					errors.append(e.getMessage());
				}

				// if the file was transported and has a checksum, check it
				// find matching METS:file element and get relevant attributes
				_filesXpath.setVariable("locator", url);
				Element metsFileEl = null;
				try {
					Object o = _filesXpath.selectSingleNode(mets);
					if (o != null && o instanceof Element) {
						metsFileEl = (Element) o;
					} else {
						throw new IngestException("Unexpected null METS file element for locator " + url);
					}
				} catch (JDOMException e) {
					throw new IngestException("Unexpected exception ", e);
				}
				// String checksumtype =
				// metsFileEl.getAttributeValue("CHECKSUMTYPE");
				String checksum = metsFileEl.getAttributeValue("CHECKSUM");
				if (checksum != null) {
					log.debug("found a checksum in METS");
					// TODO: assert same checksum in Fedora on ingest
					// transaction
					Checksum checker = new Checksum();
					try {
						String sum = checker.getChecksum(file);
						if (!sum.equals(checksum.toLowerCase())) {
							log.debug("Checksum failed for file: " + url + " (METS says " + checksum + ", but we got " + sum
									+ ")");
							badChecksumFiles.add(url);
						}
						String msg = "METS manifest checksum was verified for file: " + url;
						aip.getEventLogger().logEvent(PremisEventLogger.Type.VALIDATION, msg, pid);
					} catch (IOException e) {
						throw new IngestException("Checksum failed to find file: " + url);
					}
				}
			}
		}

		// TODO: account for local (not inline xmlData) MODS files
		// see if there are extra files in the SIP (submitted files not in
		// manifest)
		List<String> extraFiles = new ArrayList<String>();

		if (metsPack.getSIPDataDir() != null) {
			int zipPathLength = 0;
			try {
				zipPathLength = metsPack.getSIPDataDir().getCanonicalPath().length();

				for (File received : metsPack.getDataFiles()) {
					if (!manifestFiles.contains(received)) {
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
