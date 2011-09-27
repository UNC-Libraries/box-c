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
  * Encapsulates a Fedora disseminator
  */

import org.jdom.Element;
import org.jdom.Namespace; 

import java.util.List;
import java.util.Iterator;

public class Disseminator {
	protected String _id = "";
	protected String _bdefPid = "";
	protected State _state = State.ACTIVE;
	protected String _bmechPid = "";
	protected String _label = "";
	protected List<DSBinding> _datastreamBindings = null;

	public Disseminator(final String pId, final String pBdefPid, final String pBmechPid, final List<DSBinding> pDatastreamBindings) {
		this(pId, pBdefPid, State.ACTIVE, pBmechPid, "", pDatastreamBindings);
	}

	public Disseminator(final String pId, final String pBdefPid, final State pState, final String pBmechPid, final String pLabel, final List<DSBinding> pDatastreamBindings) {
		this.setId(pId);
		this.setBdefPid(pBdefPid);
		this.setState(pState);
		this.setBmechPid(pBmechPid);
		this.setLabel(pLabel);
		this.setDatastreamBindings(pDatastreamBindings);
	}

		
	/**
	 * Get id.
	 *
	 * @return id as String.
	 */
	public String getId() {
	    return _id;
	}
	
	/**
	 * Set id.
	 *
	 * @param id the value to set.
	 */
	public void setId(final String pId) {
	     _id = pId;
	}
	
	/**
	 * Get bdefPid.
	 *
	 * @return bdefPid as String.
	 */
	public String getBdefPid() {
	    return _bdefPid;
	}
	
	/**
	 * Set bdefPid.
	 *
	 * @param bdefPid the value to set.
	 */
	public void setBdefPid(final String pBdefPid) {
	     _bdefPid = pBdefPid;
	}
	
	/**
	 * Get state.
	 *
	 * @return state as State.
	 */
	public State getState() {
	    return _state;
	}
	
	/**
	 * Set state.
	 *
	 * @param state the value to set.
	 */
	public void setState(final State pState) {
	     _state = pState;
	}
	
	/**
	 * Get bmechPid.
	 *
	 * @return bmechPid as String.
	 */
	public String getBmechPid() {
	    return _bmechPid;
	}
	
	/**
	 * Set bmechPid.
	 *
	 * @param bmechPid the value to set.
	 */
	public void setBmechPid(final String pBmechPid) {
	     _bmechPid = pBmechPid;
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
	 * Get datastreamBindings.
	 *
	 * @return datastreamBindings as List of DSBinding.
	 */
	public List<DSBinding> getDatastreamBindings() {
	    return _datastreamBindings;
	}
	
	/**
	 * Set datastreamBindings.
	 *
	 * @param datastreamBindings the value to set.
	 */
	public void setDatastreamBindings(final List<DSBinding> pDatastreamBindings) {
	     _datastreamBindings = pDatastreamBindings;
	}


	/**
	 * Creates the xml for a disseminator so that it can be slotted into a FOXML ingest document
	 * @param Namespace the FOXML namespace
	 * @return Element the disseminator XML 
	 *
	 * &lt;foxml:disseminator ID="DISS1" BDEF_CONTRACT_PID="demo:8" STATE="A"&gt;
	 * 	&lt;foxml:disseminatorVersion ID="DISS1.0" BMECH_SERVICE_PID="demo:9" LABEL="Disseminator to provide dynamic views of a MRSID image"&gt;
	 * 		&lt;foxml:serviceInputMap&gt;
	 * 			&lt;foxml:datastreamBinding DATASTREAM_ID="IMAGE" KEY="MRSID" LABEL="The IMAGE datastream serves as input to the MRSID service" ORDER="0"/&gt;
	 * 		&lt;/foxml:serviceInputMap&gt;
	 * 	&lt;/foxml:disseminatorVersion&gt;
	 * &lt;/foxml:disseminator&gt;
	 *
	 */
	public Element toFOXML(final Namespace FOXML) {
		Element tDisseminatorEl = new Element("disseminator", FOXML);
		tDisseminatorEl.setAttribute("ID", this.getId());
		tDisseminatorEl.setAttribute("BDEF_CONTRACT_PID", this.getBdefPid());
		tDisseminatorEl.setAttribute("STATE", this.getState().toString());
			
		Element tDissVersionEl = new Element("disseminatorVersion", FOXML);
		tDisseminatorEl.addContent(tDissVersionEl);
		tDissVersionEl.setAttribute("ID", this.getId() + ".0");
		tDissVersionEl.setAttribute("BMECH_SERVICE_PID", this.getBmechPid());
		if (this.getLabel() != null && this.getLabel().trim().length() != 0) {
			tDissVersionEl.setAttribute("LABEL", this.getLabel());
		}

		Element tServiceInputMapEl = new Element("serviceInputMap", FOXML);
		tDissVersionEl.addContent(tServiceInputMapEl);

		Iterator<DSBinding> tDSBindingIter = this.getDatastreamBindings().iterator();
		while (tDSBindingIter.hasNext()) {
			tServiceInputMapEl.addContent(tDSBindingIter.next().toFOXML(FOXML));
		}
		
		return tDisseminatorEl;
	}
}
