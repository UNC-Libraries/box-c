package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jrdf.graph.Graph;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

public class BiomedCentralAIPFilter implements AIPIngestFilter {
	private static Logger LOG = Logger.getLogger(BiomedCentralAIPFilter.class);
	
	private static final String BIOMED_ONYEN = "biomedcentral";
	private AgentFactory agentFactory;
	
	private Agent biomedAgent;
	
	private XPath foxmlArticleXMLXPath;
	private XPath supplementXPath;
	private XPath supplementFileNameXPath;
	private XPath supplementTitle;
	
	public BiomedCentralAIPFilter(){
	}
	
	public void init(){
		biomedAgent = agentFactory.findPersonByOnyen(BIOMED_ONYEN, false);
		LOG.debug("Initializing BiomedCentralAIPFilter, retrieved biomed agent " + biomedAgent.getPID().getPid());
		try {
			foxmlArticleXMLXPath = XPath.newInstance("//f:datastream[@ID='DATA_FILE']/f:datastreamVersion[1]/f:contentLocation/@REF");
			supplementXPath = XPath.newInstance("//suppl");
			supplementFileNameXPath = XPath.newInstance("file/@name");
			supplementTitle = XPath.newInstance("text/p");
		} catch (JDOMException e) {
			LOG.error("Error initializing", e);
		}
	}
	
	
	@Override
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		LOG.debug("starting BiomedCentralAIPFilter");
		if (!biomedAgent.getPID().getPid().equals(aip.getDepositRecord().getDepositedBy().getPID().getPid())){
			LOG.debug("Deposit agent was " + aip.getDepositRecord().getDepositedBy().getPID().getPid() + ", require "
					+ biomedAgent.getPID().getPid());
			return aip;
		}
		if (!(PackagingType.METS_DSPACE_SIP_2.equals(aip.getDepositRecord().getPackagingType()) 
				|| PackagingType.METS_DSPACE_SIP_1.equals(aip.getDepositRecord().getPackagingType()))){
			LOG.debug("Packaging type " + aip.getDepositRecord().getPackagingType() + " was not applicable for filter");
			return aip;
		}
		
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		
		Graph g = rdfaip.getGraph();
		
		if (rdfaip.getTopPIDs() == null){
			throw new AIPException("The AIP contained no top level pids.");
		}
		
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
					try {
						processArticleXML(rdfaip, pid);
					} catch (Exception e){
						throw new AIPException("Unable to process article XML from " + slug, e);
					}
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
	
	private void processArticleXML(RDFAwareAIPImpl aip, PID pid) throws Exception{
		Graph g = aip.getGraph();
		
		Document foxml = aip.getFOXMLDocument(pid);
		String foxmlPath = ((Attribute)this.foxmlArticleXMLXPath.selectSingleNode(foxml)).getValue();
		File articleXMLFile = aip.getFileForUrl(foxmlPath);
		SAXBuilder sb = new SAXBuilder();
		
		Document articleDocument = sb.build(articleXMLFile);
		@SuppressWarnings("unchecked")
		List<Element> supplements = this.supplementXPath.selectNodes(articleDocument);
		if (supplements != null){
			for (Element supplement: supplements){
				String supplementFileName = ((Attribute)this.supplementFileNameXPath.selectSingleNode(supplement)).getValue();
				PID supplementPID = JRDFGraphUtil.getPIDRelationshipSubject(g, ContentModelHelper.CDRProperty.slug.getURI(), supplementFileName);
				String supplementTitle = ((Element)this.supplementTitle.selectSingleNode(supplement)).getValue().trim();
				Document supplementFOXML = aip.getFOXMLDocument(supplementPID);
				FOXMLJDOMUtil.setProperty(supplementFOXML, ObjectProperty.label, supplementTitle);
				aip.saveFOXMLDocument(supplementPID, supplementFOXML);
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
