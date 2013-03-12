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
import java.util.HashMap;
import java.util.Map;

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
		
		// get ClamScan software and database versions
		String version = this.clamScan.cmd("nVERSION\n".getBytes()).trim();
		
		Map<String, String> failures = new HashMap<String, String>();
		
		for (PID pid : aip.getPIDs()) {
			Document foxml = aip.getFOXMLDocument(pid);
			for (Element cLocation : FOXMLJDOMUtil.getFileLocators(foxml)) {
				String ref = cLocation.getAttributeValue("REF");
				String dsid = cLocation.getParentElement().getParentElement().getAttributeValue("ID");
				URI uri;
				try {
					uri = new URI(ref);
				} catch (URISyntaxException e) {
					throw new AIPException("Unable to construct URI for a METS file location.", e);
				}
				if (uri.getScheme() == null || uri.getScheme().contains("file")) {
					File file = aip.getFileForUrl(ref);
					
					ScanResult result = this.clamScan.scan(file);
					switch(result.getStatus()) {
						case FAILED:
							Element ev = aip.getEventLogger().logEvent(PremisEventLogger.Type.VIRUS_CHECK, "File failed pre-ingest scan for viruses.", pid, dsid);
							PremisEventLogger.addSoftwareAgent(ev, "ClamAV", version);
							PremisEventLogger.addDetailedOutcome(ev, "failure", "found virus signature "+result.getSignature(), null);
							failures.put(uri.toString(), result.getSignature());
							break;
						case ERROR:
							throw new AIPException("Virus checks are producing errors: "+result.getException().getLocalizedMessage());
						case PASSED:
							Element ev2 = aip.getEventLogger().logEvent(PremisEventLogger.Type.VIRUS_CHECK, "File passed pre-ingest scan for viruses.", pid, dsid);
							PremisEventLogger.addSoftwareAgent(ev2, "ClamAV", version);
							PremisEventLogger.addDetailedOutcome(ev2, "success", null, null);
							break;
					}
					
				}
			}
		}
		if(failures.size() > 0) {
			StringBuilder sb = new StringBuilder("Virus checks failed for some files:\n");
			for(String uri : failures.keySet()) {
				sb.append(uri).append(" - ").append(failures.get(uri)).append("\n");
			}
			throw new AIPException(sb.toString());
		}
		log.debug("Finished");
		return aip;
	}
}
