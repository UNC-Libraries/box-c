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
package edu.unc.lib.dl.cdr.services.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
import edu.unc.lib.dl.util.ZipFileUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.persist.services.EmailHandler;

/**
 *
 * @author harring
 * @author bbpennel
 *
 */
public class XMLExportServiceTest {

    private static final String TO_EMAIL = "to@example.com";

    @Mock
    private EmailHandler emailHandler;
    @Mock
    private AccessControlService aclService;
    @Mock
    private SearchStateFactory searchStateFactory;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private SearchState searchState;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private SearchResultResponse resultResponse;
    private XMLExportRequest request;
    @Mock
    private AccessGroupSet group;

    private String username;
    private List<String> pids;
    private XMLExportService service;

    @Captor
    private ArgumentCaptor<File> fileCaptor;
    @Mock
    private ContentObject mockContentObj;

    @Before
    public void init() {
        initMocks(this);

        username = "user";
        pids = new ArrayList<>();

        service = new XMLExportService();
        service.setAclService(aclService);
        service.setEmailHandler(emailHandler);
        service.setQueryLayer(queryLayer);
        service.setRepoObjLoader(repoObjLoader);
        service.setSearchStateFactory(searchStateFactory);

        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(Permission.bulkUpdateDescription)))
                .thenReturn(true);

        when(searchStateFactory.createSearchState()).thenReturn(searchState);

        request = new XMLExportRequest(pids, false, TO_EMAIL);
    }

    private PID registerObject() {
        PID pid = mockObject();
        pids.add(pid.getRepositoryPath());

        return pid;
    }

    private PID mockObject() {
        PID pid = PIDs.get(UUID.randomUUID().toString());

        ContentObject obj = mock(ContentObject.class);
        when(obj.getPid()).thenReturn(pid);
        when(obj.getLastModified()).thenReturn(new Date());
        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(obj);

        return pid;
    }

    @Test
    public void exportXMLTest() throws Exception {
        PID pid1 = registerObject();
        PID pid2 = registerObject();

        service.exportXml(username, group, request);

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, pid1);
        assertHasObjectWithoutMods(rootEl, pid2);
    }

    @Test
    public void exportXMLWithChildrenTest() throws Exception {
        request.setExportChildren(true);

        PID pid1 = mockObject();
        PID pid2 = mockObject();
        PID parentPid = registerObject();

        List<BriefObjectMetadata> childrenMd = new ArrayList<>();
        childrenMd.add(mockMd(pid1));
        childrenMd.add(mockMd(pid2));
        when(resultResponse.getResultList()).thenReturn(childrenMd);
        when(queryLayer.getSearchResults(any(SearchRequest.class))).thenReturn(resultResponse);

        service.exportXml(username, group, request);

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, pid1);
        assertHasObjectWithoutMods(rootEl, pid2);
        assertHasObjectWithoutMods(rootEl, parentPid);
    }

    @Test
    public void exportWithModsTest() throws Exception {
        PID pid1 = registerObject();
        ContentObject contentObj = (ContentObject) repoObjLoader.getRepositoryObject(pid1);

        BinaryObject modsObj = mock(BinaryObject.class);
        InputStream modsIs = new FileInputStream(new File(
                "src/test/resources/mods/valid-mods.xml"));
        when(modsObj.getBinaryStream()).thenReturn(modsIs);
        when(contentObj.getMODS()).thenReturn(modsObj);

        service.exportXml(username, group, request);

        Document doc = getExportedDocument();
        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        Element objEl = rootEl.getChild("object");

        assertEquals(pid1.getRepositoryPath(), objEl.getAttributeValue("pid"));

        Element updateEl = objEl.getChild("update");
        assertEquals("MODS", updateEl.getAttributeValue("type"));
        assertNotNull(updateEl.getAttributeValue("lastModified"));

        Element modsEl = updateEl.getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
        assertNotNull(modsEl);
        assertNotNull(modsEl.getChild("titleInfo", JDOMNamespaceUtil.MODS_V3_NS));
    }

    @Test
    public void exportNoPermissionTest() throws Exception {
        PID pid1 = registerObject();
        PID pid2 = registerObject();

        when(aclService.hasAccess(eq(pid1), any(AccessGroupSet.class), eq(Permission.bulkUpdateDescription)))
                .thenReturn(true);
        when(aclService.hasAccess(eq(pid2), any(AccessGroupSet.class), eq(Permission.bulkUpdateDescription)))
                .thenReturn(false);

        service.exportXml(username, group, request);

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertHasObjectWithoutMods(rootEl, pid1);
        assertNull("Object must not be present due to permissions", getObjectElByPid(rootEl, pid2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportNoObjectsTest() throws Exception {
        // Register no objects for export

        service.exportXml(username, group, request);
    }

    @Test
    public void exportNoChildrenTest() throws Exception {
        request.setExportChildren(true);

        PID parentPid = registerObject();

        when(resultResponse.getResultList()).thenReturn(Collections.emptyList());
        when(queryLayer.getSearchResults(any(SearchRequest.class))).thenReturn(resultResponse);

        service.exportXml(username, group, request);

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, parentPid);
    }

    @Test(expected = ServiceException.class)
    public void exportFailToGetChildrenTest() throws Exception {
        request.setExportChildren(true);

        // Register a parent and child object
        registerObject();
        mockObject();

        when(queryLayer.getSearchResults(any(SearchRequest.class))).thenReturn(null);

        service.exportXml(username, group, request);
    }

    @Test
    public void asynchronousExportTest() throws Exception {
        service.setAsynchronous(true);

        PID pid1 = registerObject();

        service.exportXml(username, group, request);

        verify(emailHandler, timeout(10000)).sendEmail(eq(TO_EMAIL), anyString(), anyString(),
                eq("xml_export.zip"), fileCaptor.capture());
        Document doc = getExportedDocument(fileCaptor.getValue());

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, pid1);
    }

    private void assertHasObjectWithoutMods(Element rootEl, PID pid) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertNull(objEl.getChild("update"));
    }

    private Element getObjectElByPid(Element rootEl, PID pid) {
        List<Element> objEls = rootEl.getChildren("object");
        for (Element objEl : objEls) {
            String pidAttr = objEl.getAttributeValue("pid");
            if (pid.getRepositoryPath().equals(pidAttr)) {
                return objEl;
            }
        }
        return null;
    }

    private Document getExportedDocument() throws Exception {
        verify(emailHandler).sendEmail(eq(TO_EMAIL), anyString(), anyString(),
                eq("xml_export.zip"), fileCaptor.capture());
        return getExportedDocument(fileCaptor.getValue());
    }

    private Document getExportedDocument(File reportZip) throws Exception {
        File unzipDir = ZipFileUtil.unzipToTemp(reportZip);
        File reportFile = new File(unzipDir, "export.xml");

        SAXBuilder builder = new SAXBuilder();
        return builder.build(new FileInputStream(reportFile));
    }

    private BriefObjectMetadata mockMd(PID pid) {
        BriefObjectMetadata md = mock(BriefObjectMetadata.class);
        when(md.getPid()).thenReturn(pid);
        return md;
    }
}
