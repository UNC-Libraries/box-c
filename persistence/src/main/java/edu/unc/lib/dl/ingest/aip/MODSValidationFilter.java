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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Copies certain ingest metadata into Fedora locations, such as the MODS and RELS-EXT datastreams
 * 
 * @author count0
 * 
 */
public class MODSValidationFilter implements AIPIngestFilter {
	private static final Log log = LogFactory.getLog(MODSValidationFilter.class);
	private SchematronValidator schematronValidator = null;

	public MODSValidationFilter() {
	}

	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		log.debug("starting MODSValidationFilter");
		RDFAwareAIPImpl rdfaip;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		filter(rdfaip);
		log.debug("ending MODSValidationFilter");
		return rdfaip;
	}

	public ArchivalInformationPackage filter(RDFAwareAIPImpl aip) throws AIPException {
		int invalidCounter = 0;
		for (PID pid : aip.getPIDs()) {
			Document doc = aip.getFOXMLDocument(pid);

			// if MODS exists then validate vocabularies
			// ds may be null
			Element ds = FOXMLJDOMUtil.getDatastream(doc, "MD_DESCRIPTIVE");
			if (ds != null) {
				String message = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
				Element event = aip.getEventLogger().logEvent(Type.VALIDATION, message, pid, "MD_DESCRIPTIVE");
				// extract the mods element from the datastream elements
				Element modsEl = ds.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)
						.getChild("xmlContent", JDOMNamespaceUtil.FOXML_NS).getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
				// run schematron
				Document svrl = this.getSchematronValidator().validate(new JDOMSource(modsEl), "vocabularies-mods");

				if (!this.getSchematronValidator().hasFailedAssertions(svrl)) {
					aip.getEventLogger().addDetailedOutcome(event, "MODS is valid",
							"The supplied MODS metadata meets CDR vocabulary requirements.", null);
				} else {
					aip.getEventLogger().addDetailedOutcome(event, "MODS is not valid",
							"The supplied MODS metadata does not meet CDR vocabulary requirements.", svrl.detachRootElement());
					invalidCounter++;
				}
			}

			// future: any content model specific validations
			// URI contentModelURI = JRDFGraphUtil.getContentModelType(aip.getGraph(), pid);
			// if (contentModelURI.equals(ContentModelHelper.Model.SIMPLE.getURI())) {
			// // SIMPLE object validation
			// } else {
			// String message = "MODS not valid with respect to content model";
			// aip.getEventLogger().logEvent(Type.VALIDATION, message, pid, "MD_DESCRIPTIVE");
			// missingCounter++;
			// }
		}
		if (invalidCounter > 0) {
			String message = "Some descriptive metadata (MODS) did not meet requirements.";
			throw new AIPException(message + " (" + invalidCounter + " MODS invalid)");
		}
		return aip;
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator validator) {
		this.schematronValidator = validator;
	}

}