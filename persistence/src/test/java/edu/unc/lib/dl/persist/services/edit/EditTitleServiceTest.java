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

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.*;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.*;
import java.util.Collection;
import java.util.UUID;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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
    private OperationsMessageSender operationsMessageSender;
    @Mock
    private MODSValidator modsValidator;

    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;

    private EditTitleService service;
    private PID pid;
    private Document document;

    @Before
    public void init() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        service = new EditTitleService();

        service.setAclService(aclService);
        service.setModsValidator(modsValidator);
        service.setRepoObjLoader(repoObjLoader);
        service.setOperationsMessageSender(operationsMessageSender);

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getDescription()).thenReturn(binaryObj);
        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(Permission.editDescription)))
                .thenReturn(true);
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(contentObj);
        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("agentname");
        when(operationsMessageSender.sendUpdateDescriptionOperation(anyString(), any(Collection.class))).thenReturn("message_id");

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

        verify(operationsMessageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(pid));
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
        when(binaryObj.getBinaryStream()).thenReturn(null);

        service.editTitle(agent, pid, title);

        verify(operationsMessageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(pid));
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

        verify(operationsMessageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(pid));
    }

    @Test
    public void multipleTitlesInMods() throws Exception {
        String title = "new title";
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("original title")))
                .addContent(new Element("titleInfo", MODS_V3_NS))
                        .addContent(new Element("title", MODS_V3_NS).setText("a second title")));
        when(binaryObj.getBinaryStream()).thenReturn(convertDocumentToStream(document));

        service.editTitle(agent, pid, title);

        verify(operationsMessageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(pid));
    }

    private InputStream convertDocumentToStream(Document doc) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());
    }
}
