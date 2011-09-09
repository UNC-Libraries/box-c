/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ingest.aip;

import org.jdom.Element;

/**
 * Used for exceptional behavior that can be expected while trying to complete the AIP pipeline.
 *
 * @author count0
 *
 */
public class AIPException extends Exception {
    private static final long serialVersionUID = 8226934671033531955L;

    Element errorXML = null;
    public String myMessage;

    public AIPException(final String msg) {
	super(msg);
    }

    public AIPException(final String msg, final Throwable e) {
	super(msg, e);
    }

    public Element getErrorXML() {
	return this.errorXML;
    }

    public void setErrorXML(Element errorXML) {
	this.errorXML = errorXML;
    }
}
