package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.util.List;

import org.jdom.Document;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

public class DocumentIndexingPackage {
	
	private PID pid;
	private List<DocumentIndexingPackage> parentDocuments;
	private Document foxml;
	private IndexDocumentBean document;
	
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

	public List<DocumentIndexingPackage> getParentDocuments() {
		return parentDocuments;
	}

	public void setParentDocuments(List<DocumentIndexingPackage> parentDocuments) {
		this.parentDocuments = parentDocuments;
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

	public void setSystemDates() {
		
	}
	
	/**
	 * Retrieves and stores path related attributes, including ancestorPath, ancestorNames, parentCollection, rollup
	 * 
	 *  Depends on parentDocuments or triple store
	 *  Side effect of triple store query, retrieves content models
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
	 * Sets the full text field
	 * 
	 * Same dependencies as setDatastreams and a FULL_TEXT datastream
	 */
	public void setFullText() {
		
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
	 * Sets the publication status, taking into account the publication status of its parents
	 * Published, Unpublished, UnpublishedParent
	 * 
	 * Depends on parentDocuments and RELS-EXT or triple query
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
	 * Populates the relations field with pertinent triples from RELS-EXT that are not intended for querying purposes.
	 * defaultWebData, defaultWebObject,  
	 * 
	 * Depends on FOXML/RELS-EXT or triple query
	 */
	public void setRelations() {
		
	}
	
}
