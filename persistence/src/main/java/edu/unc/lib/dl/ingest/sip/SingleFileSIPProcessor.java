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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

public class SingleFileSIPProcessor extends FileSIPProcessor {
	private static final Log log = LogFactory.getLog(SingleFileSIPProcessor.class);

	@Override
	public ArchivalInformationPackage createAIP(SubmissionInformationPackage in, DepositRecord record)
			throws IngestException {
		SingleFileSIP sip = (SingleFileSIP) in;
		// CHECK FOR MISSING OR TRUNCATED SIP DATA
		if (sip.getContainerPID() == null) {
			throw new IngestException("Please specify a container path");
		} else if (sip.getData() == null || !sip.getData().exists() || sip.getData().length() == 0) {
			throw new IngestException("Data file not found");
		} else if (sip.getModsXML() == null || !sip.getModsXML().exists() || sip.getModsXML().length() == 0) {
			throw new IngestException("MODS metadata file not found");
		}

		AIPImpl aip = prepareIngestDirectory(sip, record);

		PID pid = this.getPidGenerator().getNextPID();

		// create FOXML stub document
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());

		// parse the MODS and insert into FOXML
		String label = null;
		try {
			Document mods = new SAXBuilder().build(sip.getModsXML());
			if (log.isDebugEnabled()) {
				XMLOutputter out = new XMLOutputter();
				String output = out.outputString(mods.getRootElement());
				log.info("HERE:\n" + output);
			}
			label = ModsXmlHelper.getFormattedLabelText(mods.getRootElement());
			Element root = mods.getRootElement();
			root.detach();
			FOXMLJDOMUtil
					.setInlineXMLDatastreamContent(foxml, "MD_DESCRIPTIVE", "Descriptive Metadata (MODS)", root, true);
		} catch (JDOMException e) {
			throw new IngestException("Error parsing MODS xml.", e);
		} catch (IOException e) {
			throw new IngestException("Error reading MODS xml file.", e);
		}

		// MAKE RDF AWARE AIP
		RDFAwareAIPImpl rdfaip = null;
		try {
			rdfaip = new RDFAwareAIPImpl(aip);
			aip = null;
		} catch (AIPException e) {
			throw new IngestException("Could not create RDF AIP for simplified RELS-EXT setup of agent", e);
		}
		
		this.setDataFile(pid, sip, foxml, rdfaip);
		this.assignFileTriples(pid, sip, record, foxml, label, rdfaip);

		if (log.isDebugEnabled()) {
			rdfaip.commitGraphChanges();
			XMLOutputter out = new XMLOutputter();
			String output = out.outputString(rdfaip.getFOXMLDocument(pid));
			log.debug("HEREFOXML:\n" + output);
		}

		return rdfaip;
	}
}
