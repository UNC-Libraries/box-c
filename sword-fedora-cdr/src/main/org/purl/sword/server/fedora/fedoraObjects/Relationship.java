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
  * Date: 26th February 2009
  *
  * This encapsulates the RELS-EXT datastream and can be converted to and from XML
  *
  */

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;

public class Relationship extends InlineDatastream	{
	protected String _pid = "";
	protected List<Element> _relationship = new ArrayList<Element>();

	public final Namespace RDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	public final Namespace REL = Namespace.getNamespace("rel", "info:fedora/fedora-system:def/relations-external#");
	public final Namespace MODEL = Namespace.getNamespace("fedora-model", "info:fedora/fedora-system:def/model#");

	public Relationship() {
		super("RELS-EXT");
		super.setLabel("Relationships to other objects");
	}

	public Relationship(final Document pRDF) {
		super("RELS-EXT");
		super.setLabel("Relationships to other objects");

		Element tDescription = pRDF.getRootElement().getChild("description", RDF);

		this.setPid(tDescription.getAttributeValue("about", RDF).substring("info:fedora/".length()));
		Iterator tRelationshipsIter = tDescription.getChildren().iterator();
		Element tRelationEl = null;
		while (tRelationshipsIter.hasNext()) {
			tRelationEl = (Element)tRelationshipsIter.next();

			this.add(tRelationEl.getNamespace(), tRelationEl.getName(), tRelationEl.getAttributeValue("resource", RDF));
		}
	}

	public void addDefaultModel() {
		boolean tFound = false;
		for (Element tEl : _relationship) {
			if (tEl.getName().equals("hasModel") && tEl.getAttributeValue("resource", RDF).equals("info:fedora/fedora-system:FedoraObject-3.0")) {
				tFound = true;
			}
		}
		if (!tFound) {
			Element tDefaultModel = new Element("hasModel", MODEL);
			tDefaultModel.setAttribute("resource", "info:fedora/fedora-system:FedoraObject-3.0", RDF);
			_relationship.add(tDefaultModel);
		}
	}
	public void addModel(final String pModel) {
		boolean tFound = false;
		for (Element tEl : _relationship) {
			if (tEl.getName().equals("hasModel") && tEl.getAttributeValue("resource", RDF).equals(pModel)) {
				tFound = true;
			}
		}
		if (!tFound) {
			Element tModel = new Element("hasModel", MODEL);
			tModel.setAttribute("resource", pModel, RDF);
			_relationship.add(tModel);
		}
	}

	/**
	 * remove all fedora:model attributes for non Fedora 3 objects or
	 * else they don't ingest
	 */
	public void removeModels() {
		List<Element> tNewList = new ArrayList<Element>();
		for (Element tEl : _relationship) {
			if (tEl.getNamespace() == null || !tEl.getNamespace().getURI().equals("info:fedora/fedora-system:def/model#")) {
				tNewList.add(tEl);
			}
		}
		_relationship = tNewList;
	}

	public String getPid() {
		return _pid;
	}

	public void setPid(final String pPID) {
		_pid = pPID;
	}

	public void add(final String pType, final String pTargetPid) {
		this.add(REL, pType, pTargetPid);
	}

	/**
	 * add a relationship e.g. this object isMemeberOf collection with pid sword:99
	 * @param Namespace of the relationship, defaults to fedora relationships
	 * @param String e.g. isMemeberOf
	 * @param String the target pid  e.g.sword:99
	 */
	public void add(final Namespace pNamespace, final String pType, final String pTargetPid) {
		Element tNewRelation = new Element(pType, pNamespace);
		tNewRelation.setAttribute("resource", "info:fedora/" + pTargetPid, RDF);
		_relationship.add(tNewRelation);
	}

	public void remove(final String pType, final String pTargetPid) {
		this.remove(REL, pType, pTargetPid);
	}

	/**
	 * remove a relationship 
	 * @param Namespace of the relationship, defaults to fedora relationships
	 * @param String e.g. isMemeberOf
	 * @param String the target pid  e.g.sword:99
	 */
	public void remove(final Namespace pNamespace, final String pType, final String pTargetPid) {
		Element tRelation = null;
		for (int i = _relationship.size(); i >= 0; i--) {
			tRelation = _relationship.get(i);
			if (tRelation.getNamespace().equals(pNamespace) && tRelation.getName().equals(pType) && tRelation.getAttributeValue("resource", RDF).equals("info:fedora/" + pTargetPid)) {
				_relationship.remove(i);
			}
		}
	}


	/**
	 * Converts this datastream into XML so it can be added to FOXML
	 * @return Document the XML datastream node
	 */
	public Document toXML() {
		Document tRDF = new Document();
		Element tRoot = new Element("RDF", RDF);
		tRDF.setRootElement(tRoot);

		Element tDescription = new Element("Description", RDF);
		tRoot.addContent(tDescription);
		tDescription.setAttribute("about", "info:fedora/" + this.getPid(), RDF);

		Iterator<Element> tRelationsIter = _relationship.iterator();
		while (tRelationsIter.hasNext()) {
			tDescription.addContent(tRelationsIter.next());
		}
		
		return tRDF;
	}
}
