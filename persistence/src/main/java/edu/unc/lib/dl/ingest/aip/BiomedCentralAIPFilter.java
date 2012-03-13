package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

public class BiomedCentralAIPFilter implements AIPIngestFilter {
	private static Logger LOG = Logger.getLogger(BiomedCentralAIPFilter.class);
	
	private static final String BIOMED_ONYEN = "biomedcentral";
	private AgentFactory agentFactory;
	
	private Agent biomedAgent;
	
	private XPath foxmlArticleXMLXPath;
	private XPath supplementXPath;
	private XPath supplementFileNameXPath;
	private XPath supplementTitleXPath;
	private XPath identifierXPath;
	private XPath affiliationXPath;
	private XPath authorXPath;
	private XPath bibRootXPath;
	
	public BiomedCentralAIPFilter(){
	}
	
	public void init(){
		biomedAgent = agentFactory.findPersonByOnyen(BIOMED_ONYEN, false);
		LOG.debug("Initializing BiomedCentralAIPFilter, retrieved biomed agent " + biomedAgent.getPID().getPid());
		try {
			foxmlArticleXMLXPath = XPath.newInstance("//f:datastream[@ID='DATA_FILE']/f:datastreamVersion[1]/f:contentLocation/@REF");
			supplementXPath = XPath.newInstance("//suppl");
			supplementFileNameXPath = XPath.newInstance("file/@name");
			supplementTitleXPath = XPath.newInstance("text/p");
			identifierXPath = XPath.newInstance("xrefbib/pubidlist/pubid");
			affiliationXPath = XPath.newInstance("insg/ins");
			authorXPath = XPath.newInstance("aug/au");
			bibRootXPath = XPath.newInstance("/art/fm/bibl");
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
				if (slug.matches("^[0-9\\-X]+\\.[xX][mM][lL]$")){
					LOG.debug("Found primary Biomed XML document " + slug);
					// suppress the XML main file by turning off indexing
					JRDFGraphUtil.removeAllRelatedByPredicate(g, pid, ContentModelHelper.CDRProperty.allowIndexing.getURI());
					JRDFGraphUtil.addCDRProperty(g, pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
					try {
						processArticleXML(rdfaip, pid, parentPID);
					} catch (Exception e){
						throw new AIPException("Unable to process article XML from " + slug, e);
					}
				} else if (slug.matches("^[0-9\\-X]+\\.\\w+$")){
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
	
	@SuppressWarnings("unchecked")
	private void processArticleXML(RDFAwareAIPImpl aip, PID pid, PID parentPID) throws Exception {
		Graph g = aip.getGraph();
		
		Document foxml = aip.getFOXMLDocument(pid);
		Document parentFOXML = aip.getFOXMLDocument(parentPID);
		
		String foxmlPath = ((Attribute)this.foxmlArticleXMLXPath.selectSingleNode(foxml)).getValue();
		File articleXMLFile = aip.getFileForUrl(foxmlPath);
		SAXBuilder sb = new SAXBuilder();
		
		Document articleDocument = sb.build(articleXMLFile);
		Element bibRoot = (Element)this.bibRootXPath.selectSingleNode(articleDocument);
		
		Element modsDS = FOXMLJDOMUtil.getDatastream(parentFOXML, ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName());
		if (modsDS == null){
			modsDS = new Element(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), JDOMNamespaceUtil.MODS_V3_NS);
			modsDS.addContent(new Element("title", JDOMNamespaceUtil.MODS_V3_NS).setText(FOXMLJDOMUtil.getLabel(parentFOXML)));
		}
		
		//Get the content portion of the mods ds
		Element modsContent = modsDS.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)
				.getChild("xmlContent", JDOMNamespaceUtil.FOXML_NS).getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
		
		//Strip out preexisting names so that we can replace them.
		List<Element> preexistingModsNames = modsContent.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
		if (preexistingModsNames != null){
			Iterator<Element> it = preexistingModsNames.iterator();
			while (it.hasNext()){
				it.next();
				it.remove();
			}
		}
		
		//Add identifiers
		List<Element> elements = this.identifierXPath.selectNodes(bibRoot);
		if (elements != null){
			for (Element identifier: elements){
				String idType = identifier.getAttributeValue("idtype");
				Element modsIdentifier = new Element("identifier", JDOMNamespaceUtil.MODS_V3_NS);
				modsIdentifier.setAttribute("type", idType);
				modsIdentifier.setText(identifier.getTextTrim());
				modsContent.addContent(modsIdentifier);
			}
		}
		
		//Extract affiliations
		elements = this.affiliationXPath.selectNodes(bibRoot);
		Map<String,String> affiliationMap = new HashMap<String,String>();
		if (elements != null){
			for (Element element: elements){
				String affiliation = element.getChildTextTrim("p");
				int index = affiliation.indexOf(",");
				if (index != -1){
					affiliation = affiliation.substring(0,index);
				}
				affiliationMap.put(element.getAttributeValue("id"), affiliation);
			}
		}
		
		//Extract author names, then create name attributes with affiliations
		elements = this.authorXPath.selectNodes(bibRoot);
		if (elements != null){
			for (Element element: elements){
				String surname = element.getChildText("snm");
				String givenName = element.getChildText("fnm");
				String middle = element.getChildText("mi");
				String affiliationID = element.getChild("insr").getAttributeValue("iid");
				
				Element nameElement = new Element("name", JDOMNamespaceUtil.MODS_V3_NS);
				Element namePartElement = new Element("namePart", JDOMNamespaceUtil.MODS_V3_NS);
				
				StringBuilder nameBuilder = new StringBuilder();
				if (surname != null){
					nameBuilder.append(surname);
					if (givenName != null || middle != null)
						nameBuilder.append(", ");
				}
				if (givenName != null)
					nameBuilder.append(givenName);
				if (middle != null)
					nameBuilder.append(' ').append(middle);
				namePartElement.setText(nameBuilder.toString());
				
				nameElement.addContent(namePartElement);
				
				//Add in the affiliation if it is set.
				String affiliation = affiliationMap.get(affiliationID);
				if (affiliation != null){
					Element affiliationElement = new Element("affiliation", JDOMNamespaceUtil.MODS_V3_NS);
					affiliationElement.setText(affiliation);
					nameElement.addContent(affiliationElement);
				}
				
				modsContent.addContent(nameElement);
			}
		}
		
		// Add in the containing journal
		String source = bibRoot.getChildText("source");
		if (source != null){
			Element hostElement = new Element("relatedItem", JDOMNamespaceUtil.MODS_V3_NS);
			Element titleInfoElement = new Element("titleInfo", JDOMNamespaceUtil.MODS_V3_NS);
			Element titleElement = new Element("title", JDOMNamespaceUtil.MODS_V3_NS);
			hostElement.addContent(titleInfoElement);
			titleInfoElement.addContent(titleElement);
			hostElement.setAttribute("type", "host");
			hostElement.setAttribute("displayLabel", "Source");
			titleElement.setText(source);
			modsContent.addContent(hostElement);
		}
		
		//Set titles for supplements
		elements = this.supplementXPath.selectNodes(articleDocument);
		if (elements != null){
			for (Element supplement: elements){
				String supplementFileName = ((Attribute)this.supplementFileNameXPath.selectSingleNode(supplement)).getValue();
				PID supplementPID = JRDFGraphUtil.getPIDRelationshipSubject(g, ContentModelHelper.CDRProperty.slug.getURI(), supplementFileName);
				String supplementTitle = ((Element)this.supplementTitleXPath.selectSingleNode(supplement)).getValue().trim();
				//If the title is too long for the label field, then limit to just the main title
				if (supplementTitle.length() >= 250){
					supplementTitle = ((Element)this.supplementTitleXPath.selectSingleNode(supplement)).getChildTextTrim("b");
					//If still too long, then truncate.
					if (supplementTitle.length() >= 250){
						supplementTitle = supplementTitle.substring(0, 249);
					}
				}
				Document supplementFOXML = aip.getFOXMLDocument(supplementPID);
				FOXMLJDOMUtil.setProperty(supplementFOXML, ObjectProperty.label, supplementTitle);
				aip.saveFOXMLDocument(supplementPID, supplementFOXML);
			}
		}
		modsContent.detach();
		FOXMLJDOMUtil.setInlineXMLDatastreamContent(parentFOXML, ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), "Descriptive Metadata", modsContent, true);
		aip.saveFOXMLDocument(parentPID, parentFOXML);
	}

	public AgentFactory getAgentFactory() {
		return agentFactory;
	}

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
}
