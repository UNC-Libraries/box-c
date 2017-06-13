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
package edu.unc.lib.dl.fedora;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Gregory Jansen
 *
 */
public class StructMapOrderExtractor extends DefaultHandler {
    private PID pid = null;
    private String order = null;

    StructMapOrderExtractor(PID pid) {
    super();
    this.pid = pid;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    super.startElement(uri, localName, qName, attributes);
    if ("div".equals(localName)) {
        String id = attributes.getValue("ID");
        if (pid.getPid().equals(id)) {
        this.order = attributes.getValue("ORDER");
        }
    }
    }

    public String getOrder() {
    return this.order;
    }

}
