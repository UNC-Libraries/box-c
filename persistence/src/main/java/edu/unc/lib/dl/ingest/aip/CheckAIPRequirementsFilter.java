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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jrdf.graph.Graph;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.JRDFGraphUtil;

/**
 * Checks that every object in the AIP has a slug, owner and contentModelType. Adds the PreservedObject model to every
 * object.
 * 
 * @author count0
 * 
 */
public class CheckAIPRequirementsFilter implements AIPIngestFilter {
	private static final Log log = LogFactory.getLog(CheckAIPRequirementsFilter.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @seeedu.unc.lib.dl.ingest.IngestFilter#doFilter(edu.unc.lib.dl.ingest. IngestContextImpl)
	 */
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		log.debug("starting CheckAIPRequirementsFilter.doFilter()");
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		filter(rdfaip);
		log.debug("finished with CheckAIPRequirementsFilter.doFilter()");
		return rdfaip;
	}

	private void filter(RDFAwareAIPImpl rdfaip) throws AIPException {
		// slug, owner, modeltype
		Graph g = rdfaip.getGraph();
		for (PID pid : rdfaip.getPIDs()) {
			List<URI> types = JRDFGraphUtil.getContentModels(g, pid);
			if (types == null || types.size() == 0) {
				throw new AIPException(pid.getPid() + " missing content model type.");
			}
			List<String> owners = JRDFGraphUtil.getRelationshipLiteralObjects(g, pid,
					ContentModelHelper.Relationship.owner.getURI());
			if (owners == null || owners.size() == 0) {
				throw new AIPException(pid.getPid() + " missing owner.");
			}
			String slug = JRDFGraphUtil.getRelatedLiteralObject(g, pid, ContentModelHelper.CDRProperty.slug.getURI());
			if (slug == null) {
				throw new AIPException(pid.getPid() + " missing slug.");
			}
			// add PreservedObject model
			JRDFGraphUtil.addFedoraProperty(g, pid, FedoraProperty.hasModel,
					ContentModelHelper.Model.PRESERVEDOBJECT.getURI());
		}
	}

}
