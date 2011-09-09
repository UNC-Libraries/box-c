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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.agents.GroupAgent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.agents.SoftwareAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.pidgen.PIDGenerator;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

public class AgentSIPProcessor implements SIPProcessor {
    private static final Log log = LogFactory.getLog(AgentSIPProcessor.class);

    private PIDGenerator pidGenerator = null;
    private TripleStoreQueryService tripleStoreQueryService = null;

    @Override
    public ArchivalInformationPackage createAIP(SubmissionInformationPackage in, PremisEventLogger logger)
		    throws IngestException {
	log.debug("starting AgentSIPProcessor");
	AgentSIP sip = (AgentSIP) in;

	// GET PIDS
	Iterator<PID> newpids = this.getPidGenerator().getNextPIDs(sip.getAgents().size()).iterator();
	HashMap<PID, Agent> pid2agent = new HashMap<PID, Agent>();

	// MAKE AND SAVE FOXML DOCS, set all as top pids
	AIPImpl aip = new AIPImpl(logger);
	for (Agent p : sip.getAgents()) {
	    PID pid = newpids.next();
	    if (p.getPID() != null) {
		pid = p.getPID(); // for bootstrap agents, such as
		// admin:ADMINISTRATOR_GROUP
		// Note: agent.setPID() is package scope
	    }
	    pid2agent.put(pid, p);
	    Document pdoc = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());
	    FOXMLJDOMUtil.setProperty(pdoc, ObjectProperty.label, p.getName());
	    aip.saveFOXMLDocument(pid, pdoc);
	    if (p instanceof PersonAgent) {
		aip.setTopPIDPlacement("/admin/people", pid, null, null);
	    } else if (p instanceof GroupAgent) {
		aip.setTopPIDPlacement("/admin/groups", pid, null, null);
	    } else if (p instanceof SoftwareAgent) {
		aip.setTopPIDPlacement("/admin/software", pid, null, null);
	    }
	}

	// MAKE RDF AWARE AIP
	RDFAwareAIPImpl rdfaip = null;
	try {
	    rdfaip = new RDFAwareAIPImpl(aip);
	    aip = null;
	} catch (AIPException e) {
	    throw new IngestException("Could not create RDF AIP for simplified RELS-EXT setup of agent", e);
	}

	// SET PROPERTIES IN RDF GRAPH
	for (PID pid : rdfaip.getPIDs()) {
	    Agent a = pid2agent.get(pid);

	    PID adminGrp = AgentManager.getAdministrativeGroupAgentStub().getPID();

	    // set owner
	    JRDFGraphUtil.addFedoraPIDRelationship(rdfaip.getGraph(), pid, ContentModelHelper.Relationship.owner,
			    adminGrp);

	    // set slug
	    // Note: we use name for slug b/c not all person agents will
	    // have onyens in the long run
	    String slug = PathUtil.makeSlug(a.getName());
	    JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug, slug);
	    if (a instanceof PersonAgent) {
		// set content model
		JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.PERSONAGENT.getURI());
		PersonAgent p = (PersonAgent) a;
		// set onyen
		JRDFGraphUtil
				.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.onyen, p
						.getOnyen());
	    } else if (a instanceof GroupAgent) {
		// set content model
		JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.GROUPAGENT.getURI());
		// GroupAgent p = (GroupAgent) a;
	    } else if (a instanceof SoftwareAgent) {
		// set content model
		JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.SOFTWAREAGENT.getURI());
		// SoftwareAgent p = (SoftwareAgent) a;
	    }

	    // setup the allowIndexing property
	    JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
	}

	Set<PID> topPIDs = new HashSet<PID>();
	topPIDs.addAll(rdfaip.getPIDs());
	rdfaip.setTopPIDs(topPIDs);

	log.debug("finished with AgentSIPProcessor");
	return rdfaip;
    }

    public PIDGenerator getPidGenerator() {
	return pidGenerator;
    }

    public TripleStoreQueryService getTripleStoreQueryService() {
	return tripleStoreQueryService;
    }

    public void setPidGenerator(PIDGenerator pidGenerator) {
	this.pidGenerator = pidGenerator;
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
	this.tripleStoreQueryService = tripleStoreQueryService;
    }

}
