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
package edu.unc.lib.dl.update;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.RDFUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * 
 * @author bbpennel
 *
 */
public class RELSEXTUIPFilter extends MetadataUIPFilter {
    private static Logger log = Logger.getLogger(RELSEXTUIPFilter.class);

    private final String datastreamName = Datastream.RELS_EXT.getName();

    @Override
    public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
        // Only run this filter for metadata update requests
        if (uip == null || !(uip instanceof MetadataUIP)) {
            return uip;
        }
        MetadataUIP metadataUIP = (MetadataUIP) uip;
        log.debug("Checking " + datastreamName + " filter on " + uip.getPID().getPid());

        Object incomingObject = uip.getIncomingData().get(datastreamName);
        // Do not apply filter unless the rels-ext ds is being targeted.
        if (incomingObject == null) {
            return uip;
        }
        return this.doRelsExtFilter(metadataUIP, datastreamName, datastreamName);
    }

    protected UpdateInformationPackage doRelsExtFilter(MetadataUIP uip, String baseDatastream,
            String incomingDatastream) throws UIPException {
        if (log.isDebugEnabled()) {
            log.debug("Performing ACL " + incomingDatastream + " filter operation " + uip.getOperation().name() + " on "
                    + uip.getPID().getPid());
            try {
                System.out.println("Incoming datastreams for " + incomingDatastream);
                this.outputDatastreams(uip.getIncomingData());
                System.out.println("Modified datastreams for " + incomingDatastream);
                this.outputDatastreams(uip.getModifiedData());
                System.out.println("Original datastreams for " + incomingDatastream);
                this.outputDatastreams(uip.getOriginalData());

            } catch (IOException e) {
                log.debug("Error while outputting update datastreams", e);
            }
        }

        Element newModified = null;

        switch (uip.getOperation()) {
            case REPLACE:
                log.debug("Replacing " + baseDatastream + " with " + incomingDatastream);
                newModified = performReplace(uip, baseDatastream, incomingDatastream);
                break;
            case ADD:
                newModified = performAdd(uip, baseDatastream, incomingDatastream);
                break;
            case UPDATE:
                // Doing add for update since the schema does not allow a way to indicate a tag should replace another
                newModified = performUpdate(uip, baseDatastream, incomingDatastream);
                break;
            default:
                break;
        }

        if (newModified != null) {
            validate(uip, newModified);
            uip.getModifiedData().put(baseDatastream, newModified);
        }

        return uip;
    }

    protected Element performAdd(MetadataUIP uip, String baseDatastream, String incomingDatastream)
            throws UIPException {
        Element incoming = uip.getIncomingData().get(incomingDatastream);
        Element newModified = getNewModifiedElement(uip, baseDatastream, incoming);
        if (newModified == null) {
            return null;
        }
        return RDFUtil.mergeRDF(newModified, incoming);
    }

    protected Element performUpdate(MetadataUIP uip, String baseDatastream, String incomingDatastream)
            throws UIPException {
        Element incoming = uip.getIncomingData().get(incomingDatastream);
        Element newModified = getNewModifiedElement(uip, baseDatastream, incoming);
        if (newModified == null) {
            return null;
        }
        return RDFUtil.updateRDF(newModified, incoming);
    }

    public void validate(MetadataUIP uip, Element relsEXT) {
        // Make sure Description has rdf:about set, and that it is the object's pid
        Element descriptionElement = relsEXT.getChild("Description", JDOMNamespaceUtil.RDF_NS);
        if (descriptionElement == null || descriptionElement.getAttribute("about", JDOMNamespaceUtil.RDF_NS) == null
                || (descriptionElement.getAttribute("about", JDOMNamespaceUtil.RDF_NS) != null
                    && !uip.getPID().getURI()
                        .equals(descriptionElement.getAttributeValue("about", JDOMNamespaceUtil.RDF_NS)))) {
            Attribute aboutAttribute = new Attribute("about", uip.getPID().getURI(), JDOMNamespaceUtil.RDF_NS);
            descriptionElement.setAttribute(aboutAttribute);
        }
    }

    protected void outputDatastreams(Map<String, org.jdom2.Element> datastreamMap) throws IOException {
        if (datastreamMap == null) {
            return;
        }
        XMLOutputter outputter = new XMLOutputter();
        java.util.Iterator<java.util.Map.Entry<String, org.jdom2.Element>> it = datastreamMap.entrySet().iterator();

        while (it.hasNext()) {
            java.util.Map.Entry<String, org.jdom2.Element> element = it.next();
            if (element.getValue() == null) {
                System.out.println(element.getKey() + ": null");
                continue;
            }
            System.out.println(element.getKey() + ":\n");
            outputter.output(element.getValue(), System.out);
        }
    }
}
