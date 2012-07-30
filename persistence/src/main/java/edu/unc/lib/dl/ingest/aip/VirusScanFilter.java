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
package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;

import com.philvarner.clamavj.ClamScan;
import com.philvarner.clamavj.ScanResult;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

/**
 * This filter logs the earlier assignment of a unique identifier to each object being ingested. This identifier scheme
 * is a UUID that is compatible with both the Fedora PID and a URN.
 * 
 * @author count0
 * 
 */
public class VirusScanFilter implements AIPIngestFilter {
	private static final Log log = LogFactory.getLog(VirusScanFilter.class);
	
	private ClamScan clamScan = null;

	public ClamScan getClamScan() {
		return clamScan;
	}

	public void setClamScan(ClamScan clamScan) {
		this.clamScan = clamScan;
	}

	public VirusScanFilter() {
	}

	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		log.debug("Starting");
		for (PID pid : aip.getPIDs()) {
			Document foxml = aip.getFOXMLDocument(pid);
			for (Element cLocation : FOXMLJDOMUtil.getFileLocators(foxml)) {
				String ref = cLocation.getAttributeValue("REF");
				URI uri;
				try {
					uri = new URI(ref);
				} catch (URISyntaxException e) {
					throw new AIPException("Unable to construct URI for a METS file location.", e);
				}
				if (uri.getScheme() == null || uri.getScheme().contains("file")) {
					File file = aip.getFileForUrl(ref);
					try {
						ScanResult result = this.clamScan.scan(new FileInputStream(file));
						if(ScanResult.Status.FAILED.equals(result.getStatus())) {
							throw new AIPException("Virus check failed on "+ref+"\n"+result.getResult());
						}
						if(ScanResult.Status.ERROR.equals(result.getStatus())) {
							throw new AIPException("Virus checks are producing errors: "+result.getException().getLocalizedMessage());
						}
					} catch (FileNotFoundException e) {
						throw new AIPException("Cannot find local file referenced in METS manifest: "+ref, e);
					}
				}
			}
			aip.getEventLogger().logEvent(PremisEventLogger.Type.VIRUS_CHECK, "All packaged files passed pre-ingest scan for viruses", pid);
		}
		log.debug("Finished");
		return aip;
	}
}
