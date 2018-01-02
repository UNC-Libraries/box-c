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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

@Ignore
public class AccessControlUIPFilterTest extends Assert {

    private org.jdom2.Element getEmptyRDF() {
        org.jdom2.Element rdfElement = new org.jdom2.Element("RDF", JDOMNamespaceUtil.RDF_NS);
        org.jdom2.Element descElement = new org.jdom2.Element("Description", JDOMNamespaceUtil.RDF_NS);
        rdfElement.addContent(descElement);
        return rdfElement;
    }

    @Test
    public void aclTest() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataACL.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();
        Map<String, org.jdom2.Element> originalMap = new HashMap<>();
        originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), getEmptyRDF());
        Map<String, org.jdom2.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

        MetadataUIP uip = mock(MetadataUIP.class);
        when(uip.getPID()).thenReturn(new PID("test"));
        when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
        when(uip.getOriginalData()).thenReturn(originalMap);
        when(uip.getModifiedData()).thenReturn(originalMap);
        when(uip.getIncomingData()).thenReturn(datastreamMap);

        AccessControlUIPFilter filter = new AccessControlUIPFilter();
        filter.doFilter(uip);

        org.jdom2.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
        org.jdom2.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.allowIndexing.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertEquals("false", description.getChildText(ContentModelHelper.CDRProperty.inheritPermissions.getPredicate(),
                JDOMNamespaceUtil.CDR_ACL_NS));
        assertEquals("public", description.getChildText(UserRole.patron.getPredicate(), JDOMNamespaceUtil.CDR_ROLE_NS));
    }

    @Test
    public void publicTest() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUnpublish.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();
        Map<String, org.jdom2.Element> originalMap = new HashMap<>();
        originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), getEmptyRDF());
        Map<String, org.jdom2.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

        MetadataUIP uip = mock(MetadataUIP.class);
        when(uip.getPID()).thenReturn(new PID("test"));
        when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
        when(uip.getOriginalData()).thenReturn(originalMap);
        when(uip.getModifiedData()).thenReturn(originalMap);
        when(uip.getIncomingData()).thenReturn(datastreamMap);

        AccessControlUIPFilter filter = new AccessControlUIPFilter();
        filter.doFilter(uip);

        org.jdom2.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
        org.jdom2.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertNull(description.getChildText(ContentModelHelper.CDRProperty.allowIndexing.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertNull(description.getChildText(ContentModelHelper.CDRProperty.inheritPermissions.getPredicate(),
                JDOMNamespaceUtil.CDR_ACL_NS));
        assertNull(description.getChildText(UserRole.patron.getPredicate(), JDOMNamespaceUtil.CDR_ROLE_NS));
    }

    @Test
    public void emptyACLTest() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataEmptyACL.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();
        Map<String, org.jdom2.Element> originalMap = new HashMap<>();
        originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), getEmptyRDF());
        Map<String, org.jdom2.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

        MetadataUIP uip = mock(MetadataUIP.class);
        when(uip.getPID()).thenReturn(new PID("test"));
        when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
        when(uip.getOriginalData()).thenReturn(originalMap);
        when(uip.getModifiedData()).thenReturn(originalMap);
        when(uip.getIncomingData()).thenReturn(datastreamMap);

        AccessControlUIPFilter filter = new AccessControlUIPFilter();
        filter.doFilter(uip);

        org.jdom2.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
        org.jdom2.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

        assertEquals(0, description.getChildren().size());
    }

    @Test
    public void aclAndRELSExtTest() throws Exception {
        this.aclAndRELSExt("src/test/resources/atompub/metadataACLAndRELSEXT.xml");
    }

    @Test
    public void relsextAndACLTest() throws Exception {
        this.aclAndRELSExt("src/test/resources/atompub/metadataRELSEXTAndACL.xml");
    }

    public void aclAndRELSExt(String fileName) throws Exception {
        InputStream entryPart = new FileInputStream(new File(fileName));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();
        Map<String, org.jdom2.Element> originalMap = new HashMap<>();
        originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), getEmptyRDF());
        Map<String, org.jdom2.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

        MetadataUIP uip = mock(MetadataUIP.class);
        when(uip.getPID()).thenReturn(new PID("test"));
        when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
        when(uip.getOriginalData()).thenReturn(originalMap);
        when(uip.getModifiedData()).thenReturn(originalMap);
        when(uip.getIncomingData()).thenReturn(datastreamMap);

        RELSEXTUIPFilter relsFilter = new RELSEXTUIPFilter();
        relsFilter.doFilter(uip);

        AccessControlUIPFilter filter = new AccessControlUIPFilter();
        filter.doFilter(uip);

        org.jdom2.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
        org.jdom2.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

        assertEquals(5, description.getChildren().size());
        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertEquals(
                4,
                description.getChildren(ContentModelHelper.FedoraProperty.hasModel.name(),
                        JDOMNamespaceUtil.FEDORA_MODEL_NS).size());
    }

    @Test
    public void replacePartialMatch() throws Exception {
        org.jdom2.Element description = this.replacePartialMatch(UpdateOperation.REPLACE);

        assertEquals(2, description.getChildren().size());
        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertNull(description.getChild(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
                JDOMNamespaceUtil.CDR_ACL_NS));
        assertEquals(
                1,
                description.getChildren(ContentModelHelper.FedoraProperty.hasModel.name(),
                        JDOMNamespaceUtil.FEDORA_MODEL_NS).size());
    }

    @Test
    public void updatePartialMatch() throws Exception {
        org.jdom2.Element description = this.replacePartialMatch(UpdateOperation.UPDATE);

        assertEquals(3, description.getChildren().size());
        assertEquals("no", description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS));
        assertNotNull(description.getChild(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
                JDOMNamespaceUtil.CDR_ACL_NS));
        assertEquals(
                1,
                description.getChildren(ContentModelHelper.FedoraProperty.hasModel.name(),
                        JDOMNamespaceUtil.FEDORA_MODEL_NS).size());
    }

    public org.jdom2.Element replacePartialMatch(UpdateOperation operation) throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUnpublish.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();
        Map<String, org.jdom2.Element> originalMap = new HashMap<>();

        org.jdom2.Element rdfElement = new org.jdom2.Element("RDF", JDOMNamespaceUtil.RDF_NS);
        org.jdom2.Element descElement = new org.jdom2.Element("Description", JDOMNamespaceUtil.RDF_NS);
        rdfElement.addContent(descElement);
        org.jdom2.Element relElement = new org.jdom2.Element(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
                JDOMNamespaceUtil.CDR_NS);
        relElement.setText("yes");
        descElement.addContent(relElement);
        relElement = new org.jdom2.Element(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
                JDOMNamespaceUtil.CDR_ACL_NS);
        relElement.setText("2013-02-01");
        descElement.addContent(relElement);
        relElement = new org.jdom2.Element(ContentModelHelper.FedoraProperty.hasModel.name(),
                JDOMNamespaceUtil.FEDORA_MODEL_NS);
        relElement.setText(ContentModelHelper.Model.SIMPLE.name());
        descElement.addContent(relElement);

        originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), rdfElement);
        Map<String, org.jdom2.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

        MetadataUIP uip = mock(MetadataUIP.class);
        when(uip.getPID()).thenReturn(new PID("test"));
        when(uip.getOperation()).thenReturn(operation);
        when(uip.getOriginalData()).thenReturn(originalMap);
        when(uip.getModifiedData()).thenReturn(originalMap);
        when(uip.getIncomingData()).thenReturn(datastreamMap);

        AccessControlUIPFilter filter = new AccessControlUIPFilter();
        filter.doFilter(uip);

        org.jdom2.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
        org.jdom2.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

        return description;
    }
}
