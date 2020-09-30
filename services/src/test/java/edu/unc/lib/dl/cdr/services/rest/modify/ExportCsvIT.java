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
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
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
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryInitializer;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
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
        @ContextConfiguration("/export-csv-it-servlet.xml")
})
public class ExportCsvIT extends AbstractAPIIT {
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

        indexDummyDocument(rootObj);
        indexDummyDocument(unitObj);
        indexDummyDocument(collObj);
        indexDummyDocument(folderObj);
    }

    @Test
    public void exportCsv() throws Exception {
        String id = collObj.getPid().getUUID();
        MvcResult result = mvc.perform(get("/exportTree/csv/" + id))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("text/csv", response.getContentType());

        String expectedCsv = "Object Type,PID,Title,Path,Label,Depth,Deleted,Date Added,Date Updated,MIME Type," +
                "Checksum,File Size (bytes),Number of Children,Description\r\n" +
                "Collection," + collObj.getPid().getUUID() + ",dummy title,,,2,,,,,,,,Described\r\n";
        assertEquals(expectedCsv, response.getContentAsString());
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
        Model unitModel = ModelFactory.createDefaultModel();
        Resource unitResc = unitModel.getResource(unitPid.getRepositoryPath());
        unitResc.addProperty(CdrAcl.unitOwner, "admin");
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid, unitModel);
        rootObj.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        Model collModel = ModelFactory.createDefaultModel();
        Resource collResc = collModel.getResource(collPid.getRepositoryPath());
        collResc.addProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        collObj = repositoryObjectFactory.createCollectionObject(collPid, collModel);

        PID folderPid = pidMinter.mintContentPid();
        Model folderModel = ModelFactory.createDefaultModel();
        Resource folderResc = collModel.getResource(folderPid.getRepositoryPath());
        folderResc.addProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        folderObj = repositoryObjectFactory.createFolderObject(folderPid, folderModel);
        collObj.addMember(folderObj);

        unitObj.addMember(collObj);
    }

    private void indexDummyDocument(RepositoryObject obj) throws Exception {
        SolrInputDocument doc = new SolrInputDocument();

        doc.setField("id", obj.getPid().getId());
        doc.setField("rollup", obj.getPid().getId());

        doc.setField("title", "dummy title");

        doc.addField("adminGroup", "dummyGroup");
        doc.addField("readGroup", "dummyGroup");
        doc.addField("roleGroup", "dummyGroup");

        doc.addField("contentStatus", "Described");


        String resourceType;
        if (obj instanceof AdminUnit) {
            resourceType = ResourceType.AdminUnit.name();
            doc.addField("ancestorPath", makeAncestorPath(rootObj.getPid()));
        } else if (obj instanceof CollectionObject) {
            resourceType = ResourceType.Collection.name();
            doc.addField("ancestorPath", makeAncestorPath(unitObj.getPid(), rootObj.getPid()));
        } else if (obj instanceof FolderObject) {
            resourceType = ResourceType.Folder.name();
            doc.addField("ancestorPath", makeAncestorPath(collObj.getPid(), unitObj.getPid(),
                    rootObj.getPid()));
        } else {
            resourceType = ResourceType.ContentRoot.name();
            doc.addField("ancestorPath", "");
        }

        doc.setField("resourceType", resourceType);

        server.add(doc);
        server.commit();
    }

    private List<String> makeAncestorPath(PID... pids) {
        List<String> result = new ArrayList<>();
        int i = 0;
        for (PID pid : pids) {
            i++;
            result.add(i + "," + pid.getId());
        }
        return result;
    }
}
