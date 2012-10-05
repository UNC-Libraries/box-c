package edu.unc.lib.dl.data.ingest.solr.indexing;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.ResourceType;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class DocumentIndexingPackage {
	private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackage.class);
	
	private static XPath retrieveLabelXPath = JDOMNamespaceUtil.instantiateXPath(
			"foxml:property[@NAME='info:fedora/fedora-system:def/model#label']/@VALUE",
			new Namespace[] { Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI) });

	private static XPath retrieveObjectPropertiesXPath = JDOMNamespaceUtil.instantiateXPath("/foxml:digitalObject/foxml:objectProperties",
			new Namespace[] { Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI) });
	
	private static XPath retrieveRelsExtXPath = JDOMNamespaceUtil.instantiateXPath("/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
			+ "foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description",
			new Namespace[] { Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI), JDOMNamespaceUtil.RDF_NS });

	private PID pid;
	private DocumentIndexingPackage parentDocument;
	private Document foxml;
	private Element relsExt;
	private Element objectProperties;
	private IndexDocumentBean document;
	private String label;
	private ResourceType resourceType;

	public DocumentIndexingPackage() {
		document = new IndexDocumentBean();
	}

	public DocumentIndexingPackage(String pid) {
		this();
		this.pid = new PID(pid);
		document.setId(this.pid.getPid());
	}

	public PID getPid() {
		return pid;
	}

	public void setPid(PID pid) {
		this.pid = pid;
	}

	public DocumentIndexingPackage getParentDocument() {
		return parentDocument;
	}

	public void setParentDocument(DocumentIndexingPackage parentDocument) {
		this.parentDocument = parentDocument;
	}

	public Document getFoxml() {
		return foxml;
	}

	public void setFoxml(Document foxml) {
		this.foxml = foxml;
	}

	public IndexDocumentBean getDocument() {
		return document;
	}

	public void setDocument(IndexDocumentBean document) {
		this.document = document;
	}

	public Element getRelsExt() {
		if (relsExt == null && foxml != null) {
			try {
				setRelsExt((Element)retrieveRelsExtXPath.selectSingleNode(this.getFoxml()));
			} catch (JDOMException e) {
				return null;
			}
		}
		return relsExt;
	}

	public void setRelsExt(Element relsExt) {
		this.relsExt = relsExt;
	}

	public Element getObjectProperties() {
		if (objectProperties == null && foxml != null) {
			try {
				this.objectProperties = (Element) DocumentIndexingPackage.retrieveObjectPropertiesXPath
						.selectSingleNode(this.foxml);
			} catch (JDOMException e) {
				log.debug("Did not find object properties", e);
				return null;
			}
		}
		return this.objectProperties;
	}

	public String getLabel() {
		if (label == null && foxml != null) {
			Element objectProperties = this.getObjectProperties();
			try {
				Attribute value = (Attribute) retrieveLabelXPath.selectSingleNode(objectProperties);
				this.label = value.getValue();
			} catch (JDOMException e) {
				return null;
			}
		}
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public ResourceType getResourceType() {
		/*if (resourceType == null && document != null && document.getResourceType() != null) {
			
		}*/
		return resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public void setSystemDates() {

	}

	/**
	 * Retrieves and stores path related attributes, including ancestorPath, ancestorNames, parentCollection, rollup
	 * 
	 * Depends on parentDocuments or triple store Side effect of triple store query, retrieves content models
	 */
	public void setPath() {

	}

	/**
	 * 
	 * Depends on parentDocuments and parent MD_CONTENTS
	 */
	public void setDisplayOrder() {

	}

	/**
	 * Sets datastreams and file sizes
	 * 
	 * Depends on FOXML
	 */
	public void setDatastreams() {

	}

	/**
	 * Sets the full tExt field
	 * 
	 * Same dependencies as setDatastreams and a FULL_TExt datastream
	 */
	public void setFullTExt() {

	}

	/**
	 * Sets contentModel, resourceType, resourceTypeSort
	 * 
	 * Depends on contentModel
	 */
	public void setObjectType() {

	}

	/**
	 * Sets contentType
	 * 
	 * Depends on datastreams, resourceType, defaultWebObject
	 */
	public void setContentType() {

	}

	/**
	 * Adds datastreams from this objects default web object
	 * 
	 * Depends on FOXML of defaultWebObject if it is not the current object
	 */
	public void setDefaultWebObject() {

	}

	/**
	 * Sets roleGroup, readGroup, adminGroup
	 * 
	 * Depends on accessControlUtils or foxml and parentDocuments or foxml and retrieve parents from solr
	 */
	public void setAccessControl() {

	}

	/**
	 * Sets the publication status, taking into account the publication status of its parents Published, Unpublished,
	 * UnpublishedParent
	 * 
	 * Depends on parentDocuments and RELS-Ext or triple query
	 */
	public void setPublicationStatus() {

	}

	/**
	 * Sets all descriptive metadata fields
	 * 
	 * Depends on foxml / mods / dc
	 */
	public void setDescriptiveMetadata() {

	}

	/**
	 * Populates the relations field with pertinent triples from RELS-Ext that are not intended for querying purposes.
	 * defaultWebData, defaultWebObject,
	 * 
	 * Depends on FOXML/RELS-Ext or triple query
	 */
	public void setRelations() {

	}

}
