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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.CONTENT_DESCRIBED;
import static edu.unc.lib.dl.search.solr.util.FacetConstants.CONTENT_NOT_DESCRIBED;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.data.ingest.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryInitializer;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
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
    @javax.annotation.Resource(name = "accessGroups")
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

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;
    protected FolderObject folderObj;

    @Before
    public void setup() throws Exception {
        setupContentRoot();
        generateBaseStructure();

        treeIndexer.indexAll(baseAddress);

        setField(solrSearchService, "solrClient", server);
        setField(childrenCountService, "solrClient", server);

        solrIndexer.index(rootObj.getPid(),
                unitObj.getPid(),
                collObj.getPid(),
                folderObj.getPid());
    }

    @Test
    public void exportCsv() throws Exception {
        String id = collObj.getPid().getUUID();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("text/csv", response.getContentType());

        List<CSVRecord> csvList = parseCsvResponse(response);
        assertEquals("Unexpected number of results", 2, csvList.size());
        assertContainerRecord(csvList, ResourceType.Collection, collObj.getPid(), "Collection",
                COLLECTION_PATH, 2, false, 1, false);
        assertContainerRecord(csvList, ResourceType.Folder, folderObj.getPid(), "Folder",
                FOLDER_PATH, 3, false, null, false);
    }

    @Test
    public void exportInvalidPidCsv() throws Exception {
        mvc.perform(get("/exportTree/csv/1234"))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void exportNonExistentPidCsv() throws Exception {
        String uuid = UUID.randomUUID().toString();
        mvc.perform(get("/exportTree/csv/" + uuid))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    protected void generateBaseStructure() throws Exception {
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

        PID folderPid = pidMinter.mintContentPid();
        folderObj = repositoryObjectFactory.createFolderObject(folderPid,
                new AclModelBuilder("Folder")
                    .addCanViewOriginals(AUTHENTICATED_PRINC).model);
        collObj.addMember(folderObj);

        unitObj.addMember(collObj);
    }

    private void assertContainerRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, Integer numChildren, boolean described) {
        assertCsvRecord(csvList, objType, expectedPid, title, path, depth, deleted,
                null, null, null, numChildren, described);
    }

    private void assertCsvRecord(List<CSVRecord> csvList, ResourceType objType, PID expectedPid, String title,
            String path, int depth, boolean deleted, String mimetype, String checksum, Long fileSize,
            Integer numChildren, boolean described) {
        path = path == null ? "" : path;
        mimetype = mimetype == null ? "" : mimetype;
        checksum = checksum == null ? "" : checksum;

        for (CSVRecord rec : csvList) {
            PID pid = PIDs.get(rec.get(ExportCsvController.PID_HEADER));
            if (!pid.equals(expectedPid)) {
                continue;
            }
            assertEquals(objType.name(), rec.get(ExportCsvController.OBJ_TYPE_HEADER));
            assertEquals(path, rec.get(ExportCsvController.PATH_HEADER));
            assertEquals(depth, Integer.parseInt(rec.get(ExportCsvController.DEPTH_HEADER)));
            assertEquals(deleted, Boolean.parseBoolean(rec.get(ExportCsvController.DELETED_HEADER)));
            assertEquals(mimetype, rec.get(ExportCsvController.MIME_TYPE_HEADER));
            assertEquals(checksum, rec.get(ExportCsvController.CHECKSUM_HEADER));
            if (fileSize == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvController.FILE_SIZE_HEADER)));
            } else {
                assertEquals(fileSize, new Long(rec.get(ExportCsvController.FILE_SIZE_HEADER)));
            }
            if (numChildren == null) {
                assertTrue(StringUtils.isBlank(rec.get(ExportCsvController.NUM_CHILDREN_HEADER)));
            } else {
                assertEquals(numChildren, new Integer(rec.get(ExportCsvController.NUM_CHILDREN_HEADER)));
            }

            String expectedDescribed = described ? CONTENT_DESCRIBED : CONTENT_NOT_DESCRIBED;
            assertEquals("Unexpected description field value",
                    expectedDescribed, rec.get(ExportCsvController.DESCRIBED_HEADER));
            return;
        }
        fail("No CSV record with PID " + expectedPid.getId() + " present");
    }

    private List<CSVRecord> parseCsvResponse(MockHttpServletResponse response) throws Exception {
        List<CSVRecord> csvList = new ArrayList<>();
        System.out.println(response.getContentAsString());
        CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new StringReader(response.getContentAsString()))
                .forEach(csvList::add);
        return csvList;
    }
}
