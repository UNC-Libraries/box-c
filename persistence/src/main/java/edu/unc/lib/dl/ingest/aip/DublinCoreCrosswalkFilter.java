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

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

/**
 * Copies certain ingest metadata into Fedora locations, such as the MODS and RELS-EXT datastreams
 * 
 * @author count0
 * 
 */
public class DublinCoreCrosswalkFilter implements AIPIngestFilter {
	private static final Log log = LogFactory.getLog(DublinCoreCrosswalkFilter.class);

	public DublinCoreCrosswalkFilter() {
	}

	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		log.debug("starting Dublin Core crosswalk filter");
		for (PID pid : aip.getPIDs()) {
			Document doc = aip.getFOXMLDocument(pid);
			String source = "MD_DESCRIPTIVE";
			String dest = "DC";
			Element ds = FOXMLJDOMUtil.getDatastream(doc, source);
			Element dc = null;
			if (ds != null) {
				Element modsEl = ds.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)
						.getChild("xmlContent", JDOMNamespaceUtil.FOXML_NS).getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
				try {
					dc = ModsXmlHelper.transform(modsEl).getRootElement();
				} catch (TransformerException e) {
					throw new AIPException("There was a problem with Dublin Core metadata cross walk from MODS.", e);
				}
				String msg = "Metadata Object Description Schema (MODS) data transformed into Dublin Core (DC).";
				aip.getEventLogger().logDerivationEvent(PremisEventLogger.Type.NORMALIZATION, msg, pid, source, dest);
			} else {
				// add a DC stub datastream, with the label in the title, turn off versions
				dc = new Element("dc", JDOMNamespaceUtil.OAI_DC_NS);
				dc.addContent(new Element("title", JDOMNamespaceUtil.DC_NS).setText(FOXMLJDOMUtil.getLabel(doc)));
			}
			dc.detach();
			FOXMLJDOMUtil.setDatastreamXmlContent(doc, dest, "Internal XML Metadata", dc, false);
			aip.saveFOXMLDocument(pid, doc);
		}
		return aip;
	}
}