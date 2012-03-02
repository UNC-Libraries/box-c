package edu.unc.lib.dl.ingest.aip;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.jrdf.graph.Graph;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;

public class BiomedCentralAIPFilter implements AIPIngestFilter {
	private static Logger LOG = Logger.getLogger(BiomedCentralAIPFilter.class);
	
	private static final String BIOMED_ONYEN = "biomedcentral";
	private AgentFactory agentFactory;
	
	private Agent biomedAgent;
	
	public BiomedCentralAIPFilter(){
	}
	
	public void init(){
		biomedAgent = agentFactory.findPersonByOnyen(BIOMED_ONYEN, false);
		LOG.debug("Initializing BiomedCentralAIPFilter, retrieved biomed agent " + biomedAgent.getPID().getPid());
	}
	
	
	@Override
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		LOG.debug("starting BiomedCentralAIPFilter");
		if (!biomedAgent.getPID().getPid().equals(aip.getDepositRecord().getDepositedBy().getPID().getPid())){
			LOG.debug("Deposit agent was " + aip.getDepositRecord().getDepositedBy().getPID().getPid() + ", require "
					+ biomedAgent.getPID().getPid());
			return aip;
		}
		
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		
		Graph g = rdfaip.getGraph();
		
		// If the top level pids aren't aggregate works, then don't apply filter.
		for (PID pid: rdfaip.getTopPIDs()){
			List<URI> contentModels = JRDFGraphUtil.getContentModels(g, pid);
			boolean isAggregate = false;
			for (URI contentModel: contentModels){
				if (ContentModelHelper.Model.AGGREGATE_WORK.equals(contentModel)){
					isAggregate = true;
					break;
				}
			}
			//If the top level PID(s) is not an aggregate, then quit
			if (!isAggregate){
				LOG.debug("Deposited object " + pid.getPid() + " was not an aggregate work.");
				return rdfaip;
			}
		}
		
		filter(rdfaip);
		return rdfaip;
	}

	private void filter(RDFAwareAIPImpl rdfaip) throws AIPException {
		LOG.debug("Performing BiomedCentralAIPFilter");
		Graph g = rdfaip.getGraph();
		rdfaip.getDepositRecord().setPackagingSubType("BiomedCentral");
		
		for (PID pid: rdfaip.getPIDs()){
			PID parentPID = JRDFGraphUtil.getPIDRelationshipSubject(g, ContentModelHelper.Relationship.contains.getURI(), pid);
			if (parentPID != null){
				String slug = JRDFGraphUtil.getRelatedLiteralObject(g, pid, ContentModelHelper.CDRProperty.slug.getURI());
				LOG.debug("Processing item " + slug);
				if (slug == null) {
					throw new AIPException(pid.getPid() + " missing slug.");
				}
				if (slug.matches("^[0-9\\-]+\\.[xX][mM][lL]$")){
					LOG.debug("Found primary Biomed XML document " + slug);
					// suppress the XML main file by turning off indexing
					JRDFGraphUtil.removeAllRelatedByPredicate(g, pid, ContentModelHelper.CDRProperty.allowIndexing.getURI());
					JRDFGraphUtil.addCDRProperty(g, pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
					//rdfaip.getFOXMLDocument(pid)
				} else if (slug.matches("^[0-9\\-]+\\.\\w+$")){
					LOG.debug("Found primary Biomed XML document " + slug);
					// If this is a main object, then designate it as a default web object for its parent container
					try {
						JRDFGraphUtil.addCDRProperty(g, parentPID, ContentModelHelper.CDRProperty.defaultWebObject, new URI(pid.getURI()));
					} catch (Exception e){
						throw new AIPException("Could not add defaultWebObject triple for " + pid.getPid(), e);
					}
				}
				// Ignore supplemental files, which end in -S<#>
			}
			
		}
	}

	public AgentFactory getAgentFactory() {
		return agentFactory;
	}

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
}
