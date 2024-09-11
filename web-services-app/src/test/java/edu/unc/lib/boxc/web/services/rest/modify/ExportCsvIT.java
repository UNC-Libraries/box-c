package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.fcrepo.utils.FedoraSparqlUpdateService;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.delete.MarkForDeletionJob;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.services.processing.ExportCsvService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import javax.annotation.Resource;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.boxc.search.api.FacetConstants.CONTENT_DESCRIBED;
import static edu.unc.lib.boxc.search.api.FacetConstants.CONTENT_NOT_DESCRIBED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author lfarrell
 *
 */
@ContextHierarchy({
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
    protected PIDMinter pidMinter;
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
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Autowired
    private DerivativeService derivativeService;
    private StorageLocationTestHelper storageLocationTestHelper;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;
    protected CollectionObject collObj2;
    protected FolderObject folderObj;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setup() throws Exception {
        setupContentRoot();
        generateBaseStructure();
        storageLocationTestHelper = new StorageLocationTestHelper();
        derivativeService.setDerivativeDir(tmpFolder.toString());

        setField(solrSearchService, "solrClient", server);
        setField(childrenCountService, "solrClient", server);
    }

    @Test
    public void exportCollectionCsv() throws Exception {
        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), collObj2.getPid(), folderObj.getPid());

        String id = collObj.getPid().getId();
        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(2, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collObj.getPid(), "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Authenticated", "");
        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, null, false, "Authenticated", "");
    }

    @Test
    public void exportWorkWithFile() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
        PID folderPid = folderObj.getPid();
        String id = folderPid.getId();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(3, csvList.size(), "Unexpected number of results");

        assertCsvContentIsCorrect(csvList, folderPid, workPid, filePid);
    }

    @Test
    public void exportWorkWithAccessSurrogate() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(true, folderObj);
        PID folderPid = folderObj.getPid();
        String id = folderPid.getId();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);

        var workId = workPid.getId();
        String pathToWork = FOLDER_PATH + "/" + workId;
        assertCsvRecord(csvList, ResourceType.Work, workPid, workId,
                pathToWork, 4, false, null, null, null, null,
                1, false, "Authenticated", "", "", null, null);

        var fileTitle = "file.txt";
        String pathToFile = pathToWork + "/" + fileTitle;
        assertCsvRecord(csvList, ResourceType.File, filePid, fileTitle,
                pathToFile, 5, false, "text/plain", null, (long) 7, "Y",
                null, false, "Authenticated", "", "", getUrl(workPid.getId()), workId);
    }

    @Test
    public void exportDescribedResource() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);

        // MODS title supersedes folder name
        String pathToFolder = COLLECTION_PATH + "/Test";
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Test",
                pathToFolder, 3, false, 1, true, "Authenticated", "");

        // MODS title supersedes work name
        String pathToWork = pathToFolder + "/Work Test";
        assertCsvRecord(csvList, ResourceType.Work, workPid, "Work Test",
                pathToWork, 4, false, null, null, null, null,
                1, true, "Authenticated", "", "", null, null);

        var fileTitle = "file.txt";
        String pathToFile = pathToWork + "/" + fileTitle;
        assertCsvRecord(csvList, ResourceType.File, filePid, fileTitle,
                pathToFile, 5, false, "text/plain", null, (long) 7, null,
                null, false, "Authenticated", "", "", getUrl(workPid.getId()), "Work Test");
    }

    @Test
    public void exportDeletedResource() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        new MarkForDeletionJob(folderPid, "", getAgentPrincipals(), repositoryObjectLoader,
                sparqlUpdateService, aclService, premisLoggerFactory).run();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid, workPid, filePid);

        String id = folderPid.getId();

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);

        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, true, 1, false, "Staff-only", "");

        var workId = workPid.getId();
        String pathToWork = FOLDER_PATH + "/" + workId;
        assertCsvRecord(csvList, ResourceType.Work, workPid, workId,
                pathToWork, 4, true, null, null, null, null,
                1, false, "Staff-only", "", "", null, null);

        var fileTitle = "file.txt";
        String pathToFile = pathToWork + "/" + fileTitle;
        assertCsvRecord(csvList, ResourceType.File, filePid, fileTitle,
                pathToFile, 5, true, "text/plain", null, (long) 7, null,
                null, false, "Staff-only", "", "", getUrl(workPid.getId()), workId);
    }

    @Test
    public void exportFileResourceDirectly() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");
        String id = filePid.getId();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid, workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");

        var fileTitle = "file.txt";
        var workId = workPid.getId();
        String pathToFile = FOLDER_PATH + "/" +  workId + "/file.txt";
        assertCsvRecord(csvList, ResourceType.File, filePid, fileTitle,
                pathToFile, 5, false, "text/plain", null, (long) 7, null,
                null, false, "Authenticated", "", "", getUrl(workPid.getId()), workId);
    }

    @Test
    public void exportOneResult() throws Exception {
        PID folderPid = folderObj.getPid();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid);

        String id = folderPid.getId();

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Authenticated", "");
    }

    @Test
    public void embargoedResult() throws Exception {
        PID folderPid = pidMinter.mintContentPid();
        var embargoDate = getYearsInTheFuture(1);
        // format embargoDate value to YYYY-MM-DD
        var formatter = new SimpleDateFormat("yyyy-MM-dd");
        String expectedEmbargo = formatter.format(embargoDate.getTime());
        FolderObject folder2Obj = repositoryObjectFactory.createFolderObject(folderPid,
                new AclModelBuilder("Folder")
                        .addEmbargoUntil(embargoDate).model);
        collObj.addMember(folder2Obj);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid);

        String id = folderPid.getId();

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Restricted", expectedEmbargo);
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Public", "");
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

        MvcResult result = mvc.perform(get("/exportTree/csv/?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Authenticated", "");
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Staff-only", "");
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(2, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Public", "");
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Restricted", "");
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(2, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, 1, false, "Staff-only", "");
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, null, false, "Staff-only", "");
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

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(1, csvList.size(), "Unexpected number of results");
        assertContainerRecord(csvList, ResourceType.Collection, collPid, "Collection",
                COLLECTION_PATH, 2, false, null, false, "Restricted", "");
    }

    @Test
    public void exportContentRoot() throws Exception {
        String id = rootObj.getPid().getId();

        mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void exportInvalidPidCsv() throws Exception {
        String id = "1234";

        mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().isForbidden())
                .andReturn();

    }

    @Test
    public void exportNonExistentPidCsv() throws Exception {
        String id = UUID.randomUUID().toString();

        mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void multiPageExport() throws Exception {
        exportCsvService.setPageSize(2);

        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
        PID folderPid = folderObj.getPid();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");

        String id = unitObj.getPid().getId();

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);;

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(5, csvList.size(), "Unexpected number of results");

        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, 1, false, "Authenticated", "");

        var workId = workPid.getId();
        String pathToWork = FOLDER_PATH + "/" + workId;
        assertCsvRecord(csvList, ResourceType.Work, workPid, workId,
                pathToWork, 4, false, null, null, null, null,
                1, false, "Authenticated", "", "", null, null);

        var fileTitle = "file.txt";
        String pathToFile = pathToWork + "/" + fileTitle;
        assertCsvRecord(csvList, ResourceType.File, pidList.get("filePid"), fileTitle,
                pathToFile, 5, false, "text/plain", null, (long) 7, null,
                null, false, "Authenticated", "", "", getUrl(workPid.getId()), workId);
    }

    @Test
    public void exportWorkWithViewBehavior() throws Exception {
        Map<String, PID> pidList = addWorkToFolder(false, folderObj);
        PID folderPid = folderObj.getPid();
        String id = folderPid.getId();
        PID workPid = pidList.get("workPid");
        PID filePid = pidList.get("filePid");
        var work = repositoryObjectLoader.getWorkObject(workPid);
        var paged = ViewSettingRequest.ViewBehavior.PAGED.getString();
        repositoryObjectFactory.createExclusiveRelationship(work, CdrView.viewBehavior, paged);

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid,
                workPid, filePid);

        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(3, csvList.size(), "Unexpected number of results");

        var workId = workPid.getId();
        String pathToWork = FOLDER_PATH + "/" + workId;
        assertCsvRecord(csvList, ResourceType.Work, workPid, workId,
                pathToWork, 4, false, null, null, null, null,
                1, false, "Authenticated", "", paged, null, null);
    }

    @Test
    public void exportMultipleIds() throws Exception {
        // set up first folder object
        var folderPid1 = folderObj.getPid();
        Map<String, PID> pidList1 = addWorkToFolder(false, folderObj);
        PID workPid1 = pidList1.get("workPid");
        PID filePid1 = pidList1.get("filePid");

        // set up second folder object
        PID folderPid2 = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(folderPid2,
                new AclModelBuilder("Folder")
                        .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        collObj.addMember(folder);
        Map<String, PID> pidList2 = addWorkToFolder(false, folder);
        PID workPid2 = pidList2.get("workPid");
        PID filePid2 = pidList2.get("filePid");

        treeIndexer.indexAll(baseAddress);
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid(), folderPid1,
                workPid1, filePid1, folderPid2, workPid2, filePid2);

        var ids = folderPid1.getId() + "," +folderPid2.getId();
        MvcResult result = mvc.perform(get("/exportTree/csv?ids=" + ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertValidFileInfo(response);

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals(6, csvList.size(), "Unexpected number of results");

        assertCsvContentIsCorrect(csvList, folderPid1, workPid1, filePid1);
        assertCsvContentIsCorrect(csvList, folderPid2, workPid2, filePid2);
    }

    private Map<String, PID> addWorkToFolder(boolean accessSurrogate, FolderObject folder) throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        PID workPid = workObj.getPid();

        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        Path contentPath = createBinaryContent(bodyString);

        FileObject fileObj = repositoryObjectFactory.createFileObject(null);
        fileObj.addOriginalFile(contentPath.toUri(), filename, mimetype, null, null);
        PID filePid = fileObj.getPid();

        workObj.addMember(fileObj);

        Map<String, PID> pidList = new HashMap<>();
        pidList.put("workPid", workPid);
        pidList.put("filePid", filePid);

        if (accessSurrogate) {
            String bodyStringSurrogate = "Image";
            Path contentPathSurrogate = Files.createTempFile("surrogate", ".png");
            FileUtils.writeStringToFile(contentPath.toFile(), bodyStringSurrogate, "UTF-8");

            var surrogatePath = derivativeService.getDerivativePath(filePid, DatastreamType.ACCESS_SURROGATE);
            Files.createDirectories(surrogatePath.getParent());
            Files.copy(contentPathSurrogate, surrogatePath, StandardCopyOption.REPLACE_EXISTING);
        }

        folder.addMember(workObj);

        return pidList;
    }

    private void generateBaseStructure() {
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

    private void assertValidFileInfo(MockHttpServletResponse response) {
        String filename = "export.csv";
        assertTrue(Objects.requireNonNull(response.getHeader("Content-Disposition")).contains(filename),
                "Expected header to contain " + filename +
                        " but was " + response.getHeader("Content-Disposition"));
        assertEquals("text/csv", response.getContentType());
    }

    private void assertContainerRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, Integer numChildren, boolean described, String permissions,
                                       String embargoDate) {
        assertCsvRecord(csvList, objType, expectedPid, title, path, depth, deleted,
                null, null, null, null, numChildren, described, permissions,
                embargoDate, "", "", "");
    }

    private void assertCsvRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, String mimetype, String checksum, Long fileSize,
            String accessSurrogate, Integer numChildren, boolean described, String permissions,
            String embargoDate, String behavior, String parentWorkUrl, String parentWorkTitle) {
        path = path == null ? "" : path;
        mimetype = mimetype == null ? "" : mimetype;
        checksum = checksum == null ? "" : checksum;

        for (CSVRecord rec : csvList) {
            PID pid = PIDs.get(rec.get(ExportCsvService.PID_HEADER));
            if (!pid.equals(expectedPid)) {
                continue;
            }
            assertEquals(objType.name(), rec.get(ExportCsvService.OBJ_TYPE_HEADER));
            assertEquals(title, rec.get(ExportCsvService.TITLE_HEADER));
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
            if (accessSurrogate == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.ACCESS_SURROGATE_HEADER)));
            } else {
                assertEquals(accessSurrogate, rec.get(ExportCsvService.ACCESS_SURROGATE_HEADER));
            }
            if (numChildren == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.NUM_CHILDREN_HEADER)));
            } else {
                assertEquals(numChildren, new Integer(rec.get(ExportCsvService.NUM_CHILDREN_HEADER)));
            }

            String expectedDescribed = described ? CONTENT_DESCRIBED : CONTENT_NOT_DESCRIBED;
            assertEquals(expectedDescribed, rec.get(ExportCsvService.DESCRIBED_HEADER),
                    "Unexpected description field value");
            assertEquals(permissions, rec.get(ExportCsvService.PATRON_PERMISSIONS_HEADER));
            assertEquals(embargoDate, rec.get(ExportCsvService.EMBARGO_HEADER));
            assertEquals(behavior, rec.get(ExportCsvService.VIEW_BEHAVIOR_HEADER));

            if (parentWorkUrl == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.PARENT_WORK_URL)));
            } else {
                assertEquals(parentWorkUrl, rec.get(ExportCsvService.PARENT_WORK_URL));
            }

            if (parentWorkTitle == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvService.PARENT_WORK_TITLE)));
            } else {
                assertEquals(parentWorkTitle, rec.get(ExportCsvService.PARENT_WORK_TITLE));
            }
            return;
        }
        fail("No CSV record with PID " + expectedPid.getId() + " present");
    }

    private void assertCsvContentIsCorrect(List<CSVRecord> csvList, PID folderPid, PID workPid, PID filePid) {
        assertContainerRecord(csvList, ResourceType.Folder, folderPid, "Folder",
                FOLDER_PATH, 3, false, 1, false, "Authenticated", "");

        var workId = workPid.getId();
        String pathToWork = FOLDER_PATH + "/" + workId;
        assertCsvRecord(csvList, ResourceType.Work, workPid, workId,
                pathToWork, 4, false, null, null, null, null,
                1, false, "Authenticated", "", "", "", "");

        var fileTitle = "file.txt";
        String pathToFile = pathToWork + "/" + fileTitle;
        assertCsvRecord(csvList, ResourceType.File, filePid, fileTitle,
                pathToFile, 5, false, "text/plain", null, (long) 7, null,
                null, false, "Authenticated", "", "", getUrl(workId), workId);
    }

    private List<CSVRecord> parseCsvResponse(MockHttpServletResponse response) throws Exception {
        List<CSVRecord> csvList = new ArrayList<>();

        CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new StringReader(response.getContentAsString()))
                .forEach(csvList::add);
        return csvList;
    }

    private String getUrl(String id) {
        return "http://example.com/record/" + id;
    }
}
