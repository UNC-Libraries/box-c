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
  * This is an abstract class that is the parent for all XML inline classes. This 
  * class also extends Datastream
  */

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Namespace; 

import org.apache.log4j.Logger;

public abstract class InlineDatastream extends Datastream {
	private static final Logger LOG = Logger.getLogger(InlineDatastream.class);
	public InlineDatastream(final String pID) {
		super(pID, State.ACTIVE, ControlGroup.INTERNAL, "text/xml");
	}

	/**
	 * Converts this datastream into FOXML so it can be ingested. Calls this.toXML() on children
	 * @param Namespace the FOXML namespace
	 * @return Element the FOXML datastream node
	 */
	public Element dsToFOXML(final Namespace FOXML) {
		LOG.debug("Writing out " + super.getId() + " to FOXML");
		Element tXMLContent = new Element("xmlContent", FOXML);

		tXMLContent.addContent(this.toXML().getRootElement().detach());

		return tXMLContent;
	}

	/**
	 * This must be implemented by child classes to allow 
	 * it to be added to the FOXML
	 * 
	 * @return Document the child XML
	 */
	public abstract Document toXML();
}
