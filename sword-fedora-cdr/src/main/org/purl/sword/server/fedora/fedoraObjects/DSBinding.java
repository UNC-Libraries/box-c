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
  * This is used in the creation of disseminators and links datastreams to 
  * disseminator parameters
  *
  */

import org.jdom.Element;
import org.jdom.Namespace; 

public class DSBinding {
	protected String _dsId = "";
	protected String _key = "";
	protected String _label = "";
	protected int _order = -1;

	public DSBinding(final String pKey, final String pDsId) {
		this(pKey, pDsId, "", -1);
	}

	/**
	 * @param String the Disseminator param
	 * @param String the datastream id
	 * @param String the label to be associated with this binding
	 * @param String optional set as -1 not to add to the xml
	 */
	public DSBinding(final String pKey, final String pDsId, final String pLabel, final int pOrder) {
		this.setKey(pKey);
		this.setDsId(pDsId);
		this.setLabel(pLabel);
		this.setOrder(pOrder);
	}	
	
	/**
	 * Get dsId.
	 *
	 * @return dsId as String.
	 */
	public String getDsId() {
	    return _dsId;
	}
	
	/**
	 * Set dsId.
	 *
	 * @param dsId the value to set.
	 */
	public void setDsId(final String pDsId) {
	     _dsId = pDsId;
	}
	
	/**
	 * Get key.
	 *
	 * @return key as String.
	 */
	public String getKey() {
	    return _key;
	}
	
	/**
	 * Set key.
	 *
	 * @param key the value to set.
	 */
	public void setKey(final String pKey) {
	     _key = pKey;
	}
	
	/**
	 * Get label.
	 *
	 * @return label as String.
	 */
	public String getLabel() {
	    return _label;
	}
	
	/**
	 * Set label.
	 *
	 * @param label the value to set.
	 */
	public void setLabel(final String pLabel) {
	     _label = pLabel;
	}
	
	/**
	 * Get order.
	 *
	 * @return order as int.
	 */
	public int getOrder() {
	    return _order;
	}
	
	/**
	 * Set order.
	 *
	 * @param order the value to set.
	 */
	public void setOrder(final int pOrder) {
	     _order = pOrder;
	}


	/**
	 * Creates a XML line like the following: for addition into a FOXML document:
	 *
	 *	&lt;foxml:datastreamBinding DATASTREAM_ID="IMAGE" KEY="MRSID" LABEL="The IMAGE datastream serves as input to the MRSID service" ORDER="0"/&gt;
	 *
	 * @param Namespace the foxml namespace
	 * @return Element the elment as specified above
	 */ 
	public Element toFOXML(final Namespace FOXML) {
		Element tDatastreamBindingEl = new Element("datastreamBinding", FOXML);
		
		tDatastreamBindingEl.setAttribute("KEY", this.getKey());
		tDatastreamBindingEl.setAttribute("DATASTREAM_ID", this.getDsId());

		if (this.getLabel() != null && this.getLabel().trim().length() != 0) {
			tDatastreamBindingEl.setAttribute("LABEL", this.getLabel());
		}

		if (this.getOrder() != -1) {
			tDatastreamBindingEl.setAttribute("ORDER", "" + this.getOrder());
		}

		return tDatastreamBindingEl;
		
	}
}
