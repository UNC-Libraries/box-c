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
package edu.unc.lib.boxc.services.camel.exportxml;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.common.util.ZipFileUtil;
import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/export-xml-route-it-context.xml")
})
public class ExportXMLRouteIT {
    private static final String EMAIL = "test@example.com";

    @Autowired
    private CamelContext cdrExportXML;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private RepositoryObjectSolrIndexer solrIndexer;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected EmbeddedSolrServer server;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;
    @Autowired
    private ExportXMLRequestService requestService;
    @Autowired
    private EmailHandler emailHandler;

    @Captor
    private ArgumentCaptor<String> toCaptor;
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> bodyCaptor;
    @Captor
    private ArgumentCaptor<String> filenameCaptor;
    @Captor
    private ArgumentCaptor<File> attachmentCaptor;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj1;
    private WorkObject workObj1;
    protected CollectionObject collObj2;
    private WorkObject workObj2;
    private FileObject fileObj1;

    private AgentPrincipals agent;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        reset(emailHandler);
        TestHelper.setContentBase("http://localhost:48085/rest");
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("adminGroup"));
        generateBaseStructure();
    }

    @Test
    public void exportWorksExcludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(false, workObj1.getPid(), workObj2.getPid());

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithoutMods(rootEl, ResourceType.Work, workObj2.getPid());

        assertExportDocumentCount(rootEl, 2);
    }

    @Test
    public void exportCollectionExcludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(false, collObj1.getPid());

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Collection, collObj1.getPid());

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportCollectionIncludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(true, collObj1.getPid());

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Collection, collObj1.getPid());
        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithoutMods(rootEl, ResourceType.File, fileObj1.getPid());

        assertExportDocumentCount(rootEl, 3);
    }

    @Test
    public void exportWorksNoPermissionTest() throws Exception {
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("public"));

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenDone(1)
                .create();

        sendRequest(false, workObj1.getPid());

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();
        assertExportDocumentCount(rootEl, 0);
    }

    @Test
    public void exportCollectionNoPermissionTest() throws Exception {
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("public"));

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenDone(1)
                .create();

        sendRequest(false, collObj1.getPid());

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();
        assertExportDocumentCount(rootEl, 0);
    }

    private void indexAll() throws Exception{
        treeIndexer.indexAll(rootObj.getPid().getRepositoryPath());
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj1.getPid(), collObj2.getPid(),
                workObj1.getPid(), workObj2.getPid(), fileObj1.getPid());
    }

    private void assertEmailSent() {
        verify(emailHandler).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture(),
                filenameCaptor.capture(), attachmentCaptor.capture());
    }

    private ExportXMLRequest sendRequest(boolean exportChildren, PID... pids) throws IOException {
        ExportXMLRequest request = new ExportXMLRequest();
        request.setAgent(agent);
        request.setExportChildren(exportChildren);
        request.setPids(Arrays.stream(pids).map(PID::getId).collect(Collectors.toList()));
        request.setEmail(EMAIL);
        requestService.sendRequest(request);
        return request;
    }

    private Element getExportedDocumentRootEl() throws Exception {
        Document doc = getExportedDocument();

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());
        return rootEl;
    }

    private Document getExportedDocument() throws Exception {
        verify(emailHandler).sendEmail(eq(EMAIL), anyString(), anyString(),
                eq("xml_export.zip"), attachmentCaptor.capture());
        return getExportedDocument(attachmentCaptor.getValue());
    }

    private Document getExportedDocument(File reportZip) throws Exception {
        File unzipDir = ZipFileUtil.unzipToTemp(reportZip);
        File reportFile = new File(unzipDir, "export.xml");

        SAXBuilder builder = new SAXBuilder();
        return builder.build(new FileInputStream(reportFile));
    }

    private void assertExportDocumentCount(Element rootEl, int expectedCnt) {
        List<Element> objEls = rootEl.getChildren("object");
        assertEquals(expectedCnt, objEls.size());
    }

    private void assertHasObjectWithoutMods(Element rootEl, ResourceType expectedType, PID pid) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertEquals(expectedType.name(), objEl.getAttributeValue("type"));
        assertNull(objEl.getChild("update"));
    }

    private void assertHasObjectWithMods(Element rootEl, ResourceType expectedType, PID pid) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertEquals(expectedType.name(), objEl.getAttributeValue("type"));
        Element updateEl = objEl.getChild("update");
        assertNotNull(updateEl);
        assertNotNull(updateEl.getAttributeValue("lastModified"));
        Element modsEl = updateEl.getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
        assertNotNull(modsEl);
    }

    private Element getObjectElByPid(Element rootEl, PID pid) {
        List<Element> objEls = rootEl.getChildren("object");
        for (Element objEl : objEls) {
            String pidAttr = objEl.getAttributeValue("pid");
            if (pid.getQualifiedId().equals(pidAttr)) {
                return objEl;
            }
        }
        return null;
    }

    private void generateBaseStructure() throws Exception {
        repoInitializer.initializeRepository();
        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        PID unitPid = pidMinter.mintContentPid();
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin unit")
                    .addUnitOwner("adminGroup").model);
        rootObj.addMember(unitObj);

        PID collPid1 = pidMinter.mintContentPid();
        collObj1 = repositoryObjectFactory.createCollectionObject(collPid1,
                new AclModelBuilder("Collection 1").model);
        InputStream modsStream1 = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, collObj1, modsStream1));
        PID collPid2 = pidMinter.mintContentPid();
        collObj2 = repositoryObjectFactory.createCollectionObject(collPid2,
                new AclModelBuilder("Collection 2")
                    .addCanManage("coll2_manage").model);

        unitObj.addMember(collObj1);
        unitObj.addMember(collObj2);

        PID workPid1 = pidMinter.mintContentPid();
        workObj1 = repositoryObjectFactory.createWorkObject(workPid1, null);
        fileObj1 = workObj1.addDataFile(makeContentUri("hello"), "text.txt", "text/plain", null, null);
        InputStream modsStream2 = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workPid1, modsStream2));
        PID workPid2 = pidMinter.mintContentPid();
        workObj2 = repositoryObjectFactory.createWorkObject(workPid2, null);

        collObj1.addMember(workObj1);
        collObj2.addMember(workObj2);
    }

    protected URI makeContentUri(String content) throws Exception {
        File contentFile = File.createTempFile("test", ".txt");
        contentFile.deleteOnExit();
        FileUtils.write(contentFile, content, UTF_8);
        return contentFile.toPath().toUri();
    }

    protected InputStream streamResource(String resourcePath) throws Exception {
        return getClass().getResourceAsStream(resourcePath);
    }
}
