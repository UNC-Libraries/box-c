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
package edu.unc.lib.dl.xml;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class FOXMLJDOMUtilTest extends Assert {

    @Test
    public void getDatastreamMap() throws Exception {

        SAXBuilder builder = new SAXBuilder();
        Document foxml = builder.build(new FileInputStream(new File(
                "src/test/resources/samples/aggregateSplitDepartments.xml")));

        Map<String, Element> datastreams = FOXMLJDOMUtil.getMostRecentDatastreamMap(foxml);
        assertEquals(6, datastreams.size());
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.DC.name()));
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.name()));
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_CONTENTS.name()));
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.RELS_EXT.getName()));
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.AUDIT.name()));
        assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_EVENTS.name()));
    }

    @Test
    public void getRelationValues() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document foxml = builder.build(new FileInputStream(new File(
                "src/test/resources/samples/aggregateSplitDepartments.xml")));

        Element relsExt = FOXMLJDOMUtil.getRelsExt(foxml);
        List<String> contentModels = FOXMLJDOMUtil.getRelationValues(ContentModelHelper.FedoraProperty.hasModel.name(),
                JDOMNamespaceUtil.FEDORA_MODEL_NS, relsExt);
        assertEquals(3, contentModels.size());
        assertTrue(contentModels.contains("info:fedora/cdr-model:PreservedObject"));
        assertTrue(contentModels.contains("info:fedora/cdr-model:Container"));

        List<String> dwo = FOXMLJDOMUtil.getRelationValues(
                ContentModelHelper.CDRProperty.defaultWebObject.getPredicate(), JDOMNamespaceUtil.CDR_NS, relsExt);
        assertEquals(1, dwo.size());
        assertTrue(dwo.contains("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));

        String dewd = FOXMLJDOMUtil.getRelationValue(ContentModelHelper.CDRProperty.defaultWebData.getPredicate(),
                JDOMNamespaceUtil.CDR_NS, relsExt);
        assertNull(dewd);

        String slug = FOXMLJDOMUtil.getRelationValue(ContentModelHelper.CDRProperty.slug.getPredicate(),
                JDOMNamespaceUtil.CDR_NS, relsExt);
        assertEquals("A_Comparison_of_Machine_Learning_Algorithms_for_C", slug);

        // There are two values for this, it should always return the same first one
        String contains = FOXMLJDOMUtil.getRelationValue(ContentModelHelper.Relationship.contains.name(),
                JDOMNamespaceUtil.CDR_NS, relsExt);
        assertEquals("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d", contains);
    }
    
    @Test
    public void getMostRecentDatastream() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document foxml = builder.build(new FileInputStream(new File(
                "src/test/resources/samples/aggregateSplitDepartments.xml")));
        
        Element ds = FOXMLJDOMUtil.getMostRecentDatastream(Datastream.MD_DESCRIPTIVE, foxml);
        
        String dsid = ds.getAttributeValue("ID");
        assertEquals("MD_DESCRIPTIVE.1", dsid);
    }
    
    @Test
    public void getDatastreamContent() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document foxml = builder.build(new FileInputStream(new File(
                "src/test/resources/samples/aggregateSplitDepartments.xml")));
        
        Element ds = FOXMLJDOMUtil.getDatastreamContent(Datastream.MD_DESCRIPTIVE, foxml);
        assertEquals("mods", ds.getName());
        assertEquals(JDOMNamespaceUtil.MODS_V3_NS, ds.getNamespace());
        
        String title = ds.getChild("titleInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("title", JDOMNamespaceUtil.MODS_V3_NS).getText();
        assertEquals("Title Changed", title);
    }
}
