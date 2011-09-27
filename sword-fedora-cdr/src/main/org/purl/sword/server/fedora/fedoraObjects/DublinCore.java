package org.purl.sword.server.fedora.fedoraObjects;

/**
  * Copyright (c) 2007, Aberystwyth University
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  - Redistributions of source code must retain the above
  *    copyright notice, this list of conditions and the
  *    following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  *  - Neither the name of the Centre for Advanced Software and
  *    Intelligent Systems (CASIS) nor the names of its
  *    contributors may be used to endorse or promote products derived
  *    from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * @author Glen Robson
  * @version 1.0
  * Date: 18 October 2007 
  *
  * Encapsulates a dublin core XML file and has methods to turn it to and
  * from XML.
  * 
  */

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;

public class DublinCore extends InlineDatastream {
	protected List<String> _title = new ArrayList<String>();
	protected List<String> _creator = new ArrayList<String>();
	protected List<String> _subject = new ArrayList<String>();
	protected List<String> _description = new ArrayList<String>();
	protected List<String> _publisher = new ArrayList<String>();
	protected List<String> _contributor = new ArrayList<String>();
	protected List<String> _date = new ArrayList<String>();
	protected List<String> _type = new ArrayList<String>();
	protected List<String> _format = new ArrayList<String>();
	protected List<String> _identifier = new ArrayList<String>();
	protected List<String> _source = new ArrayList<String>();
	protected List<String> _language = new ArrayList<String>();
	protected List<String> _relation = new ArrayList<String>();
	protected List<String> _coverage = new ArrayList<String>();
	protected List<String> _rights = new ArrayList<String>();


	public final Namespace OAI_DC = Namespace.getNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/"); 
	public final Namespace DC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

	public DublinCore() {
		super("DC");
		super.setLabel("Dublin Core Metadata");
	}

	/**
	 * Create object from Dublin Core XML
	 * @param Document XML document
	 */
	public DublinCore(final Document pDcXML) {
		super("DC");
		super.setLabel("Dublin Core Metadata");

		Element tDCRoot = pDcXML.getRootElement();

		this.createFromXML(tDCRoot, "title", this.getTitle());
		this.createFromXML(tDCRoot, "creator", this.getCreator());
		this.createFromXML(tDCRoot, "subject", this.getSubject());
		this.createFromXML(tDCRoot, "description", this.getDescription());
		this.createFromXML(tDCRoot, "publisher", this.getPublisher());
		this.createFromXML(tDCRoot, "contributor", this.getContributor());
		this.createFromXML(tDCRoot, "date", this.getDate());
		this.createFromXML(tDCRoot, "type", this.getType());
		this.createFromXML(tDCRoot, "format", this.getFormat());
		this.createFromXML(tDCRoot, "identifier", this.getIdentifier());
		this.createFromXML(tDCRoot, "source", this.getSource());
		this.createFromXML(tDCRoot, "language", this.getLanguage());
		this.createFromXML(tDCRoot, "relation", this.getRelation());
		this.createFromXML(tDCRoot, "coverage", this.getCoverage());
		this.createFromXML(tDCRoot, "rights", this.getRights());

	}

	protected void createFromXML(final Element pDCRoot, final String pNodeName, final List<String> pValueStore) {
		Iterator tChildrenIter = pDCRoot.getChildren(pNodeName, DC).iterator();
		Element tElement = null;
		while (tChildrenIter.hasNext()) {
			tElement = (Element)tChildrenIter.next();
			pValueStore.add(tElement.getText());
		}
	}

	public List<String> getTitle() {
		return _title;
	}

	public void setTitle(final List<String> pTitle) {
		_title = pTitle;
	}
	
	public List<String> getCreator() {
		return _creator;
	}

	public void setCreator(final List<String> pCreator) {
		_creator = pCreator;
	}

	public List<String> getSubject() {
		return _subject;
	}

	public void setSubject(final List<String> pSubject) {
		_subject = pSubject;
	}

	public List<String> getDescription() {
		return _description;
	}

	public void setDescription(final List<String> pDescription) {
		_description = pDescription;
	}

	public List<String> getPublisher() {
		return _publisher;
	}

	public void setPublisher(final List<String> pPublisher) {
		_publisher = pPublisher;
	}

	public List<String> getContributor() {
		return _contributor;
	}

	public void setContributor(final List<String> pContributor) {
		_contributor = pContributor;
	}

	public List<String> getDate() {
		return _date;
	}

	public void setDate(final List<String> pDate) {
		_date = pDate;
	}

	public List<String> getType() {
		return _type;
	}

	public void setType(final List<String> pType) {
		_type = pType;
	}

	public List<String> getFormat() {
		return _format;
	}

	public void setFormat(final List<String> pFormat) {
		_format = pFormat;
	}

	public List<String> getIdentifier() {
		return _identifier;
	}

	public void setIdentifier(final List<String> pIdentifier) {
		_identifier = pIdentifier;
	}

	public List<String> getSource() {
		return _source;
	}

	public void setSource(final List<String> pSource) {
		_source = pSource;
	}

	public List<String> getLanguage() {
		return _language;
	}

	public void setLanguage(final List<String> pLanguage) {
		_language = pLanguage;
	}

	public List<String> getRelation() {
		return _relation;
	}

	public void setRelation(final List<String> pRelation) {
		_relation = pRelation;
	}

	public List<String> getCoverage() {
		return _coverage;
	}

	public void setCoverage(final List<String> pCoverage) {
		_coverage = pCoverage;
	}

	public List<String> getRights() {
		return _rights;
	}

	public void setRights(final List<String> pRights) {
		_rights = pRights;
	}

	/**
	 * Express this object as Dublin Core XML
	 * @return Document the XML
	 */
	public Document toXML() {
		Document tDC = new Document();
		Element tRoot = new Element("dc", OAI_DC);
		tDC.setRootElement(tRoot);

		this.listToXML(tRoot, "title", this.getTitle());
		this.listToXML(tRoot, "creator", this.getCreator());
		this.listToXML(tRoot, "subject", this.getSubject());
		this.listToXML(tRoot, "description", this.getDescription());
		this.listToXML(tRoot, "publisher", this.getPublisher());
		this.listToXML(tRoot, "contributor", this.getContributor());
		this.listToXML(tRoot, "date", this.getDate());
		this.listToXML(tRoot, "type", this.getType());
		this.listToXML(tRoot, "format", this.getFormat());
		this.listToXML(tRoot, "identifier", this.getIdentifier());
		this.listToXML(tRoot, "source", this.getSource());
		this.listToXML(tRoot, "language", this.getLanguage());
		this.listToXML(tRoot, "relation", this.getRelation());
		this.listToXML(tRoot, "coverage", this.getCoverage());
		this.listToXML(tRoot, "rights", this.getRights());

		return tDC;
	}

	protected void listToXML(final Element pParent, final String pName, final List<String>pValues) {
		Iterator<String> tValuesIter = pValues.iterator();
		while (tValuesIter.hasNext()) {
			pParent.addContent(new Element(pName, DC).addContent(tValuesIter.next()));
		}
	}

}
