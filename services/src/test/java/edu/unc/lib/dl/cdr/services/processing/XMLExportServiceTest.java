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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
import edu.unc.lib.dl.util.ZipFileUtil;
import edu.unc.lib.persist.services.EmailHandler;

/**
 *
 * @author harring
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
    SearchResultResponse resultResponse;
    @Mock
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
        when(request.getPids()).thenReturn(pids);

        service = new XMLExportService();
        service.setAclService(aclService);
        service.setEmailHandler(emailHandler);
        service.setQueryLayer(queryLayer);
        service.setRepoObjLoader(repoObjLoader);
        service.setSearchStateFactory(searchStateFactory);

        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(Permission.bulkUpdateDescription)))
                .thenReturn(true);
        when(request.getEmail()).thenReturn(TO_EMAIL);

        when(searchStateFactory.createSearchState()).thenReturn(searchState);
    }

    private PID registerObject() {
        PID pid = PIDs.get(UUID.randomUUID().toString());
        pids.add(pid.getRepositoryPath());

        ContentObject obj = mock(ContentObject.class);
        when(obj.getPid()).thenReturn(pid);
        when(obj.getLastModified()).thenReturn(new Date());
        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(obj);

        return pid;
    }

    @Test
    public void exportXMLTest() throws Exception {
        when(request.getExportChildren()).thenReturn(false);

        PID pid1 = registerObject();
        PID pid2 = registerObject();

        Map<String,String> response = service.exportXml(username, group, request);

        assertEquals("Metadata export for " + request.getPids().size()
                + " objects has begun, you will receive the data via email soon", response.get("message"));

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, pid1);
        assertHasObjectWithoutMods(rootEl, pid2);
    }

    @Test
    public void exportXMLWithChildrenTest() throws Exception {
        when(request.getExportChildren()).thenReturn(true);

        PID pid1 = registerObject();
        PID pid2 = registerObject();
        PID parentPid = registerObject();

        List<BriefObjectMetadata> childrenMd = new ArrayList<>();
        childrenMd.add(mockMd(pid1));
        childrenMd.add(mockMd(pid2));
        when(resultResponse.getResultList()).thenReturn(childrenMd);
        when(queryLayer.getSearchResults(any(SearchRequest.class))).thenReturn(resultResponse);

        BriefObjectMetadata parentMd = mockMd(parentPid);
        when(queryLayer.addSelectedContainer(anyString(), eq(searchState), any(Boolean.class)))
                .thenReturn(parentMd);

        Map<String,String> response = service.exportXml(username, group, request);

        assertEquals("Metadata export for " + request.getPids().size()
                + " objects has begun, you will receive the data via email soon", response.get("message"));

        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());

        assertHasObjectWithoutMods(rootEl, pid1);
        assertHasObjectWithoutMods(rootEl, pid2);
        assertHasObjectWithoutMods(rootEl, parentPid);
    }

    @Test
    public void exportWithModsTest() throws Exception {

    }

    private void assertHasObjectWithoutMods(Element rootEl, PID pid) {
        List<Element> objEls = rootEl.getChildren("object");
        for (Element objEl : objEls) {
            String pidAttr = objEl.getAttributeValue("pid");
            if (pid.getRepositoryPath().equals(pidAttr)) {
                assertNull(objEl.getChild("update"));
                return;
            }
        }
        fail("Did not contain expected child object " + pid);
    }

    private Document getExportedDocument() throws Exception {
        verify(emailHandler).sendEmail(eq(TO_EMAIL), anyString(), anyString(),
                eq("xml_export.zip"), fileCaptor.capture());
        File reportZip = fileCaptor.getValue();
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
