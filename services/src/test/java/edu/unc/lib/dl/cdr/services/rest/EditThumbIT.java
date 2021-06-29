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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.dl.acl.util.Permission.editDescription;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.fcrepo4.AccessControlServiceImpl;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.cdr.services.processing.ImportThumbnailService;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.MessageSender;

/**
 * @author lfarrell
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/add-thumb-it-servlet.xml")
})
public class EditThumbIT extends AbstractAPIIT {
    private static final String USER_NAME = "user";
    private static final String ADMIN_GROUP = "adminGroup";
    private CollectionObject collection;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Captor
    private ArgumentCaptor<Document> docCaptor;

    @Autowired
    private ImportThumbnailService service;
    @Autowired
    private AccessControlServiceImpl aclServices;
    @Autowired
    private MessageSender messageSender;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;

    private File tempDir;

    @Before
    public void init_() throws Exception {
        tempDir = tmpFolder.newFolder();
        service.setSourceImagesDir(tempDir.getAbsolutePath());
        service.init();
    }

    @Before
    public void setup() {
        initMocks(this);
        reset(messageSender);

        AccessGroupSet testPrincipals = new AccessGroupSet(ADMIN_GROUP);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);

        setupContentRoot();
        collection = repositoryObjectFactory.createCollectionObject(null);
    }

    @Test
    public void addEditThumbnail() throws Exception {
        FileInputStream input = new FileInputStream("src/test/resources/upload-files/burndown.png");
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "burndown.png", "image/png", IOUtils.toByteArray(input));

        mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/displayThumbnail/" + collection.getPid().getUUID()))
                .file(thumbnailFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        assertMessageValues(msgDoc, collection.getPid());
    }

    @Test
    public void addCollectionThumbWrongFileType() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "file.txt", "plain/text", textStream());

        mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/displayThumbnail/" + collection.getPid().getUUID()))
                .file(thumbnailFile))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void addCollectionThumbNoAccess() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "file.txt", "plain/text", textStream());

        doThrow(new AccessRestrictionException()).when(aclServices)
                .assertHasAccess(anyString(), eq(collection.getPid()), any(AccessGroupSet.class), eq(editDescription));

        mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/displayThumbnail/" + collection.getPid().getUUID()))
                .file(thumbnailFile))
                .andExpect(status().isForbidden())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    private byte[] textStream() {
        return "I am not an image".getBytes();
    }

    private void assertMessageValues(Document msgDoc, PID expectedPid) {
        Element entry = msgDoc.getRootElement();
        String pidString = entry.getChild(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS)
                .getChildText("pid", CDR_MESSAGE_NS);
        String author = entry.getChild("author", ATOM_NS)
                .getChildText("name", ATOM_NS);

        assertEquals(collection.getPid().getURI(), pidString);
        assertEquals(USER_NAME, author);
    }
}
