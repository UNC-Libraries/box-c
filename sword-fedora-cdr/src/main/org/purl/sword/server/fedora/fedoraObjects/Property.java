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
  * This allows you to set an object's properties in Fedora
  *
  */

import org.jdom.Element;
import org.jdom.Namespace; 

public class Property {
	public enum TYPE { INTERNAL, EXTERNAL };

	protected String _name;
	protected String _value;
	protected TYPE _type;

	public Property(final String pName, final String pValue) {
		this(pName, pValue, TYPE.INTERNAL); 
	}

	public Property(final String pName, final String pValue, final TYPE pType) {
		this.setName(pName);
		this.setValue(pValue);
		this.setType(pType);
	}
	
	/**
	 * Get name.
	 *
	 * @return name as String.
	 */
	public String getName() {
	    return _name;
	}
	
	/**
	 * Set name.
	 *
	 * @param name the value to set.
	 */
	public void setName(final String pName) {
	     _name = pName;
	}
	
	/**
	 * Get value.
	 *
	 * @return value as String.
	 */
	public String getValue() {
	    return _value;
	}
	
	/**
	 * Set value.
	 *
	 * @param value the value to set.
	 */
	public void setValue(final String pValue) {
	     _value = pValue;
	}
	
	/**
	 * Get type.
	 *
	 * @return type as TYPE.
	 */
	public TYPE getType() {
	    return _type;
	}
	
	/**
	 * Set type.
	 *
	 * @param type the value to set.
	 */
	public void setType(final TYPE pType) {
	     _type = pType;
	}

	/**
	 * Converts this datastream into FOXML so it can be ingested. 
	 * @param Namespace the FOXML namespace
	 * @return Element the FOXML datastream node
	 */
	public Element toFOXML(final Namespace FOXML) {
		Element tPropertyEl = null;

		if (this.getType().equals(TYPE.INTERNAL)) {
			tPropertyEl = new Element("property", FOXML);
		} else {
			tPropertyEl = new Element("extproperty", FOXML);
		}

		if (this.getName() == null || this.getValue() == null) {
			throw new IllegalArgumentException("Please ensure you set both name and value to actual content rather than null. Currently name=" + this.getName() + ", value=" + this.getValue());
		}
		tPropertyEl.setAttribute("NAME", this.getName());
		tPropertyEl.setAttribute("VALUE", this.getValue());

		return tPropertyEl;
	}
}
