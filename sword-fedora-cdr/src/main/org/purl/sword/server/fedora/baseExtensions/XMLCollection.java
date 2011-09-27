package org.purl.sword.server.fedora.baseExtensions;

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
  * This extends the base file and allows it to be created from a XML config file
  */

import org.purl.sword.base.Collection;

import org.jdom.Element;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class XMLCollection extends Collection {
	private static final Logger LOG = Logger.getLogger(XMLCollection.class);

	protected String _collectionPid = "";

@SuppressWarnings(value={"unchecked"})
	public XMLCollection(final Element pCollectionEl) {
		super();

		_collectionPid = pCollectionEl.getAttributeValue("collection_pid");
		
		String tLocation = pCollectionEl.getChild("deposit_url").getText();
		super.setLocation(tLocation.replaceAll("##COLLECTION_PID##", _collectionPid));

		super.setTitle(pCollectionEl.getChild("title").getText());

		for (Element tAcceptsEl : (List<Element>)pCollectionEl.getChild("accepts").getChildren()) {
			super.addAccepts(tAcceptsEl.getText());
		}

		super.setCollectionPolicy(pCollectionEl.getChild("policy").getText());
		super.setMediation(this.convertToBoolean(pCollectionEl.getAttributeValue("mediation")));

		super.setTreatment(pCollectionEl.getChild("treatment").getText());

		for (Element tAcceptsPackageEl : (List<Element>)pCollectionEl.getChild("packaging").getChildren()) {
			super.addAcceptPackaging(tAcceptsPackageEl.getText(), Float.parseFloat(tAcceptsPackageEl.getAttributeValue("quality")));
		}

		super.setAbstract(pCollectionEl.getChild("abstract").getText());

		if (pCollectionEl.getChild("service") != null) {
			super.setService(pCollectionEl.getChild("service").getText());
		}
	}

	public String getCollectionPid() {
		return _collectionPid;
	}

	private boolean convertToBoolean(final String pBoolean) {
		if (pBoolean.toLowerCase().equals("true")) {
			return true;
		} else if (pBoolean.toLowerCase().equals("false")) {
			return false;
		} else {
			throw new IllegalArgumentException("Value must be true or false you supplied '" + pBoolean + "'");
		}
	}
}
