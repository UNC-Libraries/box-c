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
package edu.unc.lib.boxc.integration.web.common;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.NamespaceConstants.FITS_URI;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DCTerms;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.test.RepositoryObjectSolrIndexer;

/**
 * @author bbpennel
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
    @ContextConfiguration("/loris-content-it-servlet.xml")
})
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class LorisContentIT {

    @Autowired
    protected String baseAddress;
    @Autowired
    protected WebApplicationContext context;
    @Autowired
    protected AccessControlService aclService;
    @Autowired
    protected GlobalPermissionEvaluator globalPermEvaluator;
    @Autowired
    protected RepositoryObjectFactory repoObjFactory;
    @Autowired
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private RepositoryObjectSolrIndexer solrIndexer;
    @Autowired
    protected RepositoryInitializer repoInitializer;
    @Autowired
    private DerivativeService derivativeService;

    protected ContentRootObject contentRoot;

    protected MockMvc mvc;

    protected AdminUnit unitObj;
    protected CollectionObject collObj;

    @Before
    public void setup() throws Exception {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));

        generateBaseStructure();
    }

    @After
    public void tearDown() {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testGetManifestFileWithJp2() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(new AclModelBuilder("Work").model);
        collObj.addMember(workObj);
        FileObject fileObj = addFileObject(workObj, true);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid(),
                workObj.getPid(), fileObj.getPid());

        // Both the file and the work should return an image manifest
        assertHasImageManifest(fileObj.getPid(), fileObj.getPid(), "file", "file");
        assertHasImageManifest(workObj.getPid(), fileObj.getPid(), "Work", "file");

        // Should produce the same result with a primary object
        workObj.setPrimaryObject(fileObj.getPid());
        treeIndexer.indexAll(workObj.getPid().getRepositoryPath());
        solrIndexer.index(workObj.getPid(), fileObj.getPid());
        assertHasImageManifest(workObj.getPid(), fileObj.getPid(), "Work", "file");
    }

    @Test
    public void testGetManifestNoImage() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(new AclModelBuilder("Work3").model);
        collObj.addMember(workObj);
        FileObject fileObj = addFileObject(workObj, false);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid(),
                workObj.getPid(), fileObj.getPid());

        // Neither object should return a manifest
        assertNoManifest(fileObj.getPid());
        assertEmptyManifest(workObj.getPid(), "Work3");
    }

    @Test
    public void testGetManifestCollection() throws Exception {
        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid());

        assertNoManifest(collObj.getPid());
    }

    @Test
    public void testGetManifestMultipleFiles() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        collObj.addMember(workObj);
        FileObject fileObj = addFileObject(workObj, true);
        FileObject fileObj2 = addFileObject(workObj, "file2.txt", false, null);
        FileObject fileObj3 = addFileObject(workObj, "file3.png", true, null);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid(),
                workObj.getPid(), fileObj.getPid(), fileObj2.getPid(), fileObj3.getPid());

        Manifest manifest = callGetManifest(workObj.getPid());
        assertEquals(2, manifest.getSequences().get(0).getCanvases().size());
        assertManifestContainsImage(manifest, 0, fileObj.getPid(), "file");
        assertManifestContainsImage(manifest, 1, fileObj3.getPid(), "file3.png");

        workObj.setPrimaryObject(fileObj.getPid());
        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(fileObj.getPid(), fileObj2.getPid(), fileObj3.getPid());
    }

    @Test
    public void testGetManifestPrimaryObjectNonImage() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(new AclModelBuilder("Work4").model);
        collObj.addMember(workObj);
        FileObject fileObj = addFileObject(workObj, "file2.txt", false, null);
        FileObject fileObj2 = addFileObject(workObj, "file3.png", true, null);
        workObj.setPrimaryObject(fileObj.getPid());

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid(),
                workObj.getPid(), fileObj.getPid(), fileObj2.getPid());

        assertHasImageManifest(workObj.getPid(), fileObj2.getPid(), "Work4", "file3.png");
    }

    @Test
    public void testGetManifestJp2MetadataOnly() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSet(PUBLIC_PRINC));

        WorkObject workObj = repoObjFactory.createWorkObject(new AclModelBuilder("Work2").model);
        collObj.addMember(workObj);
        FileObject fileObj = addFileObject(workObj, "file", true, UserRole.canViewMetadata);
        FileObject fileObj2 = addFileObject(workObj, "file2", true, null);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(contentRoot.getPid(), unitObj.getPid(), collObj.getPid(),
                workObj.getPid(), fileObj.getPid(), fileObj2.getPid());

        assertHasImageManifest(workObj.getPid(), fileObj2.getPid(), "Work2", "file2");
        assertHasImageManifest(fileObj2.getPid(), fileObj2.getPid(), "file2", "file2");
        assertNoAccess(fileObj.getPid());
    }

    private void assertNoManifest(PID pid) throws Exception {
        mvc.perform(get("/jp2Proxy/" + pid.getId() + "/jp2/manifest"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private void assertNoAccess(PID pid) throws Exception {
        mvc.perform(get("/jp2Proxy/" + pid.getId() + "/jp2/manifest"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    private Manifest callGetManifest(PID pid) throws Exception {
        MvcResult result = mvc.perform(get("/jp2Proxy/" + pid.getId() + "/jp2/manifest"))
                .andExpect(status().isOk())
                .andReturn();

        return parseManifestResponse(result);
    }

    private void assertEmptyManifest(PID pid, String label) throws Exception {
        Manifest manifest = callGetManifest(pid);
        Sequence sequence = manifest.getSequences().get(0);
        assertTrue(CollectionUtils.isEmpty(sequence.getCanvases()));
        assertEquals(label, manifest.getLabelString());
    }

    private void assertHasImageManifest(PID pid, PID filePid, String label, String fileLabel) throws Exception {
        Manifest manifest = callGetManifest(pid);
        assertEquals(label, manifest.getLabelString());
        Sequence sequence = manifest.getSequences().get(0);
        List<Canvas> canvases = sequence.getCanvases();
        assertEquals(1, canvases.size());
        assertManifestContainsImage(manifest, 0, filePid, fileLabel);
    }

    private void assertManifestContainsImage(Manifest manifest, int index, PID filePid, String label) {
        Sequence sequence = manifest.getSequences().get(0);
        List<Canvas> canvases = sequence.getCanvases();
        Canvas canvas = canvases.get(index);
        assertEquals(label, canvas.getLabelString());
        assertEquals(375, canvas.getHeight().intValue());
        assertEquals(250, canvas.getWidth().intValue());
        List<Annotation> images = canvas.getImages();
        assertEquals(2, images.size());
        Annotation jp2Image = images.get(0);
        assertEquals("http://localhost:48085/jp2Proxy/" + filePid.getId() + "/jp2",
                jp2Image.getResource().getServices().get(0).getIdentifier().toString());
        Annotation thumbImage = images.get(1);
        assertEquals("http://localhost:48085/services/api/thumb/" + filePid.getId() + "/large",
                thumbImage.getResource().getIdentifier().toString());
    }

    private Manifest parseManifestResponse(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        Manifest manifest = new IiifObjectMapper().readValue(response.getContentAsString(), Manifest.class);
        return manifest;
    }

    private FileObject addFileObject(WorkObject workObj, boolean isImage) throws Exception {
        return addFileObject(workObj, "file", isImage, null);
    }

    private FileObject addFileObject(WorkObject workObj, String filename, boolean isImage, UserRole role)
            throws Exception {
        String bodyString = "Content";
        String mimetype = isImage ? "image/png" : "text/plain";
        Path contentPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), bodyString, "UTF-8");

        Model model = null;
        if (UserRole.canViewMetadata.equals(role)) {
            model = new AclModelBuilder(filename).addCanViewMetadata(PUBLIC_PRINC).model;
        } else if (UserRole.none.equals(role)) {
            model = new AclModelBuilder(filename).addNoneRole(PUBLIC_PRINC).model;
        }

        FileObject fileObj = repoObjFactory.createFileObject(model);
        fileObj.addOriginalFile(contentPath.toUri(), filename, mimetype, null, null);
        PID filePid = fileObj.getPid();

        if (isImage) {
            PID fitsPid = DatastreamPids.getTechnicalMetadataPid(filePid);
            fileObj.addBinary(fitsPid, this.getClass().getResource("/datastream/techmd_image.xml").toURI(),
                    TECHNICAL_METADATA.getDefaultFilename(), TECHNICAL_METADATA.getMimetype(),
                    null, null, IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));
            Path jp2Path = derivativeService.getDerivativePath(filePid, DatastreamType.JP2_ACCESS_COPY);
            Files.createDirectories(jp2Path.getParent());
            Files.createFile(jp2Path);
        }

        workObj.addMember(fileObj);
        return fileObj;
    }

    private void generateBaseStructure() throws Exception {
        repoInitializer.initializeRepository();
        contentRoot = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        PID unitPid = pidMinter.mintContentPid();
        unitObj = repoObjFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin unit")
                    .addUnitOwner("admin").model);
        contentRoot.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        collObj = repoObjFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        unitObj.addMember(collObj);
    }

}
