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
package edu.unc.lib.dl.persist.services.edit;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

public class EditTitleServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private ContentObject contentObj;
    @Mock
    private BinaryObject binaryObj;
    @Mock
    private UpdateDescriptionService updateDescriptionService;

    @Captor
    private ArgumentCaptor<PID> pidCaptor;
    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    private EditTitleService service;
    private PID pid;
    private Document document;

    @Before
    public void init() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        service = new EditTitleService();

        service.setAclService(aclService);
        service.setUpdateDescriptionService(updateDescriptionService);
        service.setRepoObjLoader(repoObjLoader);

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getDescription()).thenReturn(binaryObj);
        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(Permission.editDescription)))
                .thenReturn(true);
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(contentObj);
        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("agentname");

        document = new Document();
    }

    @Test
    public void editTitleTest() throws Exception {
        String title = "new title";
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("original title"))));
        when(binaryObj.getBinaryStream()).thenReturn(convertDocumentToStream(document));

        service.editTitle(agent, pid, title);

        verify(updateDescriptionService).updateDescription(
                eq(agent), pidCaptor.capture(), inputStreamCaptor.capture());

        assertEquals(pid, pidCaptor.getValue());

        Document updatedDoc = getUpdatedDescriptionDocument();
        assertTrue(hasTitleValue(updatedDoc, title));
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() throws Exception {
        String title = "new title";
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("original title"))));
        when(binaryObj.getBinaryStream()).thenReturn(convertDocumentToStream(document));

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), any(Permission.class));

        service.editTitle(agent, pid, title);
    }

    @Test
    public void noModsTest() throws Exception {
        String title = "new title";
        when(contentObj.getDescription()).thenReturn(null);

        service.editTitle(agent, pid, title);

        verify(updateDescriptionService).updateDescription(
                eq(agent), pidCaptor.capture(), inputStreamCaptor.capture());

        assertEquals(pid, pidCaptor.getValue());

        Document updatedDoc = getUpdatedDescriptionDocument();
        assertTrue(hasTitleValue(updatedDoc, title));
    }

    @Test
    public void noTitleInMods() throws Exception {
        String title = "new title";
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("language", MODS_V3_NS)
                        .addContent(new Element("languageTerm", MODS_V3_NS)
                                .setText("eng")
                                .setAttribute("authority", "iso639-2b")
                                .setAttribute("type", "code"))));
        when(binaryObj.getBinaryStream()).thenReturn(convertDocumentToStream(document));

        service.editTitle(agent, pid, title);

        verify(updateDescriptionService).updateDescription(
                eq(agent), pidCaptor.capture(), inputStreamCaptor.capture());

        assertEquals(pid, pidCaptor.getValue());

        Document updatedDoc = getUpdatedDescriptionDocument();
        assertTrue(hasTitleValue(updatedDoc, title));
    }

    @Test
    public void multipleTitlesInMods() throws Exception {
        String title = "new title";
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("original title")))
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("a second title"))));
        when(binaryObj.getBinaryStream()).thenReturn(convertDocumentToStream(document));

        service.editTitle(agent, pid, title);

        verify(updateDescriptionService).updateDescription(
                eq(agent), pidCaptor.capture(), inputStreamCaptor.capture());

        assertEquals(pid, pidCaptor.getValue());


        Document updatedDoc = getUpdatedDescriptionDocument();
        assertTrue(hasTitleValue(updatedDoc, title));
        // check that first title is no longer in mods
        assertFalse(hasTitleValue(updatedDoc, "original title"));
        // check that second title is unchanged
        assertTrue(hasTitleValue(updatedDoc, "a second title"));
    }

    private InputStream convertDocumentToStream(Document doc) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());
    }

    private Document getUpdatedDescriptionDocument() throws IOException, JDOMException {
        SAXBuilder sb = new SAXBuilder(new XMLReaderSAX2Factory(false));
        return sb.build(inputStreamCaptor.getValue());
    }

    private boolean hasTitleValue(Document document, String expectedTitle) {
        return document.getRootElement()
                .getChildren("titleInfo", MODS_V3_NS)
                .stream()
                .anyMatch(e -> (e.getChild("title", MODS_V3_NS).getValue().contentEquals(expectedTitle)));
    }
}
