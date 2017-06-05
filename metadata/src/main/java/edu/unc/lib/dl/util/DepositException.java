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
package edu.unc.lib.dl.util;

import org.jdom2.Element;

/**
 * @author bbpennel
 * @date Mar 24, 2014
 */
public class DepositException extends Exception implements XMLAttachedException {

    private static final long serialVersionUID = -4065348103957132332L;

    Element errorXML = null;

    public DepositException(String msg) {
        super(msg);
    }

    public DepositException(String msg, Throwable e) {
        super(msg, e);
        if (e instanceof XMLAttachedException) {
            this.errorXML = ((XMLAttachedException) e).getErrorXML();
        }
    }

    @Override
    public Element getErrorXML() {
        return errorXML;
    }

    public void setErrorXML(Element errorXML) {
        this.errorXML = errorXML;
    }

}