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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.CONTENT_DESCRIBED;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.CONTENT_NOT_DESCRIBED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.cdr.services.processing.ExportCsvService;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.data.ingest.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryInitializer;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.delete.MarkForDeletionJob;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.sparql.FedoraSparqlUpdateService;
import edu.unc.lib.dl.test.AclModelBuilder;
import edu.unc.lib.dl.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.dl.util.ResourceType;

/**
 *
 * @author lfarrell
 *
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/export-csv-it-servlet.xml")
})
public class ExportCsvIT extends AbstractAPIIT {
    private static final String COLLECTION_PATH =
            "/Content Collections Root/Admin unit/Collection";
    private static final String FOLDER_PATH =
            "/Content Collections Root/Admin unit/Collection/Folder";
    private static final Path MODS_PATH_1 = Paths.get("src/test/resources/mods/valid-mods.xml");
    private static final Path MODS_PATH_2 = Paths.get("src/test/resources/mods/work-mods.xml");

    @Autowired
    protected String baseAddress;
    @Autowired
    protected File solrDataDir;
    @Autowired
    protected EmbeddedSolrServer server;
    @Autowired
    protected ChildrenCountService childrenCountService;
    @Autowired
    protected SolrUpdateDriver driver;
    @Autowired
    protected SolrSearchService solrSearchService;
    @Resource(name = "accessGroups")
    protected AccessGroupSet accessGroups;
    @Autowired
    protected Model queryModel;
    @Autowired
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    protected DocumentIndexingPackageFactory dipFactory;
    @Autowired
    protected RepositoryPIDMinter pidMinter;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private RepositoryObjectSolrIndexer solrIndexer;
    @Autowired
    private FedoraSparqlUpdateService sparqlUpdateService;
    @Autowired
    private UpdateDescriptionService updateDescService;
    @Autowired
    private ExportCsvService exportCsvService;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;
    protected CollectionObject collObj2;
    protected FolderObject folderObj;

    @Before
    public void setup() throws Exception {
        setupContentRoot();
        generateBaseStructure();

        setField(solrSearchService, "solrClient", server);
        setField(childrenCountService, "solrClient", server);
    }

    @Test
    public void exportCollectionCsv() throws Exception {
        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), collObj2.getPid(), folderObj.getPid());

        String id = collObj.getPid().getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 2, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collObj.getPid(), "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Authenticated", false);
        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, null, false, "Authenticated", false);
    }

    @Test
    public void exportWorkWithFile() throws Exception {
        Map<String, PID> pidList = addWorkToFolder();
        PID folderPid = folderObj.getPid();
        String id = folderPid.getId();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 3, csvList.size());

        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, 1, false, "Authenticated", false);

        String pathToWork = FOLDER_PATH + "/" + workPid.getId();
        assertCsvRecord(csvList, ResourceType.Work, workPid, "TestWork",
                pathToWork, 4, false, null, null, null,
                1, false, "Authenticated", false);

        String pathToFile = pathToWork + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, pidList.get("filePid"), "TestWork",
                pathToFile, 5, false, "text/plain", null, (long) 7,
                null, false, "Authenticated", false);
    }

    @Test
    public void exportDescribedResource() throws Exception {
        Map<String, PID> pidList = addWorkToFolder();
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        updateDescService.updateDescription(new UpdateDescriptionRequest(
                getAgentPrincipals(), folderPid, Files.newInputStream(MODS_PATH_1)));
        updateDescService.updateDescription(new UpdateDescriptionRequest(
                getAgentPrincipals(), workPid, Files.newInputStream(MODS_PATH_2)));

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        String id = folderPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);

        // MODS title supersedes folder name
        String pathToFolder = COLLECTION_PATH + "/Test";
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Test",
                pathToFolder, 3, false, 1, true, "Authenticated", false);

        // MODS title supersedes work name
        String pathToWork = pathToFolder + "/Work Test";
        assertCsvRecord(csvList, ResourceType.Work, workPid, "Work Test",
                pathToWork, 4, false, null, null, null,
                1, true, "Authenticated", false);

        String pathToFile = pathToWork + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, filePid, "TestWork2",
                pathToFile, 5, false, "text/plain", null, (long) 7,
                null, false, "Authenticated", false);
    }

    @Test
    public void exportDeletedResource() throws Exception {
        Map<String, PID> pidList = addWorkToFolder();
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        new MarkForDeletionJob(folderPid, "", getAgentPrincipals(), repositoryObjectLoader,
                sparqlUpdateService, aclService).run();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid, workPid, filePid);

        String id = folderPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);

        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, true, 1, false, "Staff-only", false);

        String pathToWork = FOLDER_PATH + "/" + workPid.getId();
        assertCsvRecord(csvList, ResourceType.Work, workPid, "TestWorkDeleted",
                pathToWork, 4, true, null, null, null,
                1, false, "Staff-only", false);

        String pathToFile = pathToWork + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, filePid, "TestWork2",
                pathToFile, 5, true, "text/plain", null, (long) 7,
                null, false, "Staff-only", false);
    }

    @Test
    public void exportFileResourceDirectly() throws Exception {
        Map<String, PID> pidList = addWorkToFolder();
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");
        String id = filePid.getId();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid, workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());

        String pathToFile = FOLDER_PATH + "/" + workPid.getId() + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, filePid, "TestWork3",
                pathToFile, 5, false, "text/plain", null, (long) 7,
                null, false, "Authenticated", false);
    }

    @Test
    public void exportOneResult() throws Exception {
        PID folderPid = folderObj.getPid();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid);

        String id = folderPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Authenticated", false);
    }

    @Test
    public void embargoedResult() throws Exception {
        PID folderPid = pidMinter.mintContentPid();
        FolderObject folder2Obj = repositoryObjectFactory.createFolderObject(folderPid,
                new AclModelBuilder("Folder")
                        .addEmbargoUntil(getYearsInTheFuture(1)).model);
        collObj.addMember(folder2Obj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid);

        String id = folderPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Restricted", true);
    }

    @Test
    public void publicPermission() throws Exception {
        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                        .addCanViewOriginals(PUBLIC_PRINC).model);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Public", false);
    }

    @Test
    public void authenticatedPermission() throws Exception {
        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                        .addCanViewOriginals(AUTHENTICATED_PRINC)
                        .addNoneRole(PUBLIC_PRINC).model);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Authenticated", false);
    }

    @Test
    public void staffOnlyPermission() throws Exception {
        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                        .addNoneRole(AUTHENTICATED_PRINC)
                        .addNoneRole(PUBLIC_PRINC).model);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Staff-only", false);
    }

    @Test
    public void moreRestrictiveChildPermission() throws Exception {
        PID folderPid = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(folderPid, new AclModelBuilder("Folder")
                .addCanViewMetadata(PUBLIC_PRINC).model);

        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection").addCanViewOriginals(PUBLIC_PRINC).model);

        collObj.addMember(folder);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folder.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 2, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Public", false);
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Restricted", false);
    }

    @Test
    public void childInheritsParentPermission() throws Exception {
        PID folderPid = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(folderPid, new AclModelBuilder("Folder")
                .addCanViewOriginals(PUBLIC_PRINC).model);

        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection").addNoneRole(PUBLIC_PRINC).model);

        collObj.addMember(folder);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folder.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 2, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Staff-only", false);
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Staff-only", false);
    }

    @Test
    public void RestrictedPermission() throws Exception {
        PID collPid = pidMinter.mintContentPid();
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                        .addCanViewMetadata(AUTHENTICATED_PRINC)
                        .addNoneRole(PUBLIC_PRINC).model);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid());

        String id = collPid.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 1, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Restricted", false);
    }

    @Test
    public void exportContentRoot() throws Exception {
        String id = rootObj.getPid().getId();
        mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void exportInvalidPidCsv() throws Exception {
        mvc.perform(get("/exportTree/csv/1234"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void exportNonExistentPidCsv() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void multiPageExport() throws Exception {
        exportCsvService.setPageSize(2);

        Map<String, PID> pidList = addWorkToFolder();
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        String id = unitObj.getPid().getId();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response, id);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 5, csvList.size());

        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, 1, false, "Authenticated", false);

        String pathToWork = FOLDER_PATH + "/" + workPid.getId();
        assertCsvRecord(csvList, ResourceType.Work, workPid, "TestWork",
                pathToWork, 4, false, null, null, null,
                1, false, "Authenticated", false);

        String pathToFile = pathToWork + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, pidList.get("filePid"), "TestWork",
                pathToFile, 5, false, "text/plain", null, (long) 7,
                null, false, "Authenticated", false);
    }

    private Map<String, PID> addWorkToFolder() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        PID workPid = workObj.getPid();

        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        Path contentPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), bodyString, "UTF-8");

        FileObject fileObj = repositoryObjectFactory.createFileObject(null);
        fileObj.addOriginalFile(contentPath.toUri(), filename, mimetype, null, null);
        PID filePid = fileObj.getPid();

        workObj.addMember(fileObj);

        folderObj.addMember(workObj);

        Map<String, PID> pidList = new HashMap<>();
        pidList.put("workPid", workPid);
        pidList.put("filePid", filePid);

        return pidList;
    }

    private void generateBaseStructure() throws Exception {
        repoInitializer.initializeRepository();
        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        PID unitPid = pidMinter.mintContentPid();
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin unit")
                    .addUnitOwner("admin").model);
        rootObj.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        collObj = repositoryObjectFactory.createCollectionObject(collPid,
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        PID collPid2 = pidMinter.mintContentPid();
        collObj2 = repositoryObjectFactory.createCollectionObject(collPid2,
                new AclModelBuilder("Collection2")
                        .addCanViewOriginals(AUTHENTICATED_PRINC).model);

        PID folderPid = pidMinter.mintContentPid();
        folderObj = repositoryObjectFactory.createFolderObject(folderPid,
                new AclModelBuilder("Folder")
                    .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        collObj.addMember(folderObj);

        unitObj.addMember(collObj);
        unitObj.addMember(collObj2);
    }

    private Calendar getYearsInTheFuture(int numYears) {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 365 * numYears);
        return c;
    }

    private void assertValidFileInfo(MockHttpServletResponse response, String id) {
        String filename = String.format("\"%s.csv\"", id);
        assertTrue("Expected header to end with " + filename + " but was " + response.getHeader("Content-Disposition"),
                response.getHeader("Content-Disposition").endsWith(filename));
        assertEquals("text/csv", response.getContentType());
    }

    private void assertContainerRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, Integer numChildren, boolean described, String permissions,
                                       boolean embargoed) {
        assertCsvRecord(csvList, objType, expectedPid, title, path, depth, deleted,
                null, null, null, numChildren, described, permissions, embargoed);
    }

    private void assertCsvRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, String mimetype, String checksum, Long fileSize,
            Integer numChildren, boolean described, String permissions, boolean embargoed) {
        path = path == null ? "" : path;
        mimetype = mimetype == null ? "" : mimetype;
        checksum = checksum == null ? "" : checksum;

        for (CSVRecord rec : csvList) {
            PID pid = PIDs.get(rec.get(ExportCsvService.PID_HEADER));
            if (!pid.equals(expectedPid)) {
                continue;
            }
            assertEquals(objType.name(), rec.get(ExportCsvService.OBJ_TYPE_HEADER));
            assertEquals(path, rec.get(ExportCsvService.PATH_HEADER));
            assertEquals(depth, Integer.parseInt(rec.get(ExportCsvService.DEPTH_HEADER)));
            assertEquals(deleted, Boolean.parseBoolean(rec.get(ExportCsvService.DELETED_HEADER)));
            assertEquals(mimetype, rec.get(ExportCsvService.MIME_TYPE_HEADER));
            assertEquals(checksum, rec.get(ExportCsvService.CHECKSUM_HEADER));
            if (fileSize == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.FILE_SIZE_HEADER)));
            } else {
                assertEquals(fileSize, new Long(rec.get(ExportCsvService.FILE_SIZE_HEADER)));
            }
            if (numChildren == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.NUM_CHILDREN_HEADER)));
            } else {
                assertEquals(numChildren, new Integer(rec.get(ExportCsvService.NUM_CHILDREN_HEADER)));
            }

            String expectedDescribed = described ? CONTENT_DESCRIBED : CONTENT_NOT_DESCRIBED;
            assertEquals("Unexpected description field value",
                    expectedDescribed, rec.get(ExportCsvService.DESCRIBED_HEADER));
            assertEquals(permissions, rec.get(ExportCsvService.PATRON_PERMISSIONS_HEADER));
            assertEquals(embargoed, Boolean.parseBoolean(rec.get(ExportCsvService.EMBARGO_HEADER)));
            return;
        }
        fail("No CSV record with PID " + expectedPid.getId() + " present");
    }

    private List<CSVRecord> parseCsvResponse(MockHttpServletResponse response) throws Exception {
        List<CSVRecord> csvList = new ArrayList<>();

        CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new StringReader(response.getContentAsString()))
                .forEach(csvList::add);
        return csvList;
    }
}
