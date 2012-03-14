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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jrdf.graph.Graph;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Model;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

/**
 * Create a deposit record object and references it from every item in this
 * deposit. Note that deposit objects are not in a container, they are only
 * distinguished by their content model URI.
 * 
 * @author count0
 * 
 */
public class SetOriginalDepositRecordFilter implements AIPIngestFilter {
	private static final Log log = LogFactory
			.getLog(SetOriginalDepositRecordFilter.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @seeedu.unc.lib.dl.ingest.IngestFilter#doFilter(edu.unc.lib.dl.ingest.
	 * IngestContextImpl)
	 */
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip)
			throws AIPException {
		log.debug("starting SetOriginalDepositIDFilter");
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		filter(rdfaip);
		log.debug("finished with SetOriginalDepositIDFilter.doFilter()");
		return rdfaip;
	}

	private void filter(RDFAwareAIPImpl rdfaip) throws AIPException {
		// slug, owner, modeltype
		Graph g = rdfaip.getGraph();
		for (PID pid : rdfaip.getPIDs()) {
			// add original deposit ID
			JRDFGraphUtil.addFedoraPIDRelationship(g, pid,
					Relationship.originalDeposit, rdfaip.getDepositRecord()
							.getPid());
		}
		DepositRecord dr = rdfaip.getDepositRecord();
		rdfaip.getEventLogger().logEvent(Type.NORMALIZATION,
				"Deposit recorded", dr.getPid());
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(dr.getPid().getPid());
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.label, "Deposit by "
				+ dr.getDepositedBy().getName() + " via "
				+ dr.getMethod().getLabel());
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "Active");
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.ownerId, "fedoraAdmin");
		// add manifest data stream
		if (dr.getManifest() != null) {
			Element locator = FOXMLJDOMUtil.makeLocatorDatastream(
					"DATA_MANIFEST", "M", dr.getManifest().getName(),
					"text/xml", "URL", "Deposit Manifest", false, null);
			foxml.getRootElement().addContent(locator);
		}
		rdfaip.saveFOXMLDocument(dr.getPid(), foxml);
		JRDFGraphUtil.addFedoraProperty(g, dr.getPid(), FedoraProperty.hasModel, Model.DEPOSIT_RECORD.getURI());
		if (dr.getDepositedBy() != null) {
			JRDFGraphUtil.addFedoraPIDRelationship(g, dr.getPid(),
					Relationship.depositedBy, dr.getDepositedBy().getPID());
		}
		if (dr.getOnBehalfOf() != null) {
			JRDFGraphUtil.addCDRProperty(g, dr.getPid(),
					CDRProperty.depositedOnBehalfOf, dr.getOnBehalfOf());
		}
		if (dr.getMethod() != null) {
			JRDFGraphUtil.addCDRProperty(g, dr.getPid(),
					CDRProperty.depositMethod, dr.getMethod().getLabel());
		}
		if (dr.getPackagingType() != null) {
			JRDFGraphUtil.addCDRProperty(g, dr.getPid(),
					CDRProperty.depositPackageType, dr.getPackagingType()
							.toURI());
		}
		if (dr.getPackagingSubType() != null) {
			JRDFGraphUtil
					.addCDRProperty(g, dr.getPid(),
							CDRProperty.depositPackageSubType,
							dr.getPackagingSubType());
		}
	}

}
