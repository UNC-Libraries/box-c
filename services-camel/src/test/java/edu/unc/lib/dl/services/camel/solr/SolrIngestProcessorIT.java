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
package edu.unc.lib.dl.services.camel.solr;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.ResourceType;

/**
 *
 * @author bbpennel
 *
 */
public class SolrIngestProcessorIT extends AbstractSolrProcessorIT {

    private SolrIngestProcessor processor;

    @Autowired
    private DocumentIndexingPipeline solrFullUpdatePipeline;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        processor = new SolrIngestProcessor(dipFactory, solrFullUpdatePipeline, driver, 5, 100);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @Test
    public void testIndexDescribedWork() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        String contentText = "Content";
        FileObject fileObj = workObj.addDataFile(new ByteArrayInputStream(contentText.getBytes()),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());
        workObj.setDescription(getClass().getResourceAsStream("/datastreams/simpleMods.xml"));

        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);

        setMessageTarget(workObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid().getId(), accessGroups);
        BriefObjectMetadata workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertNotNull("Date added must be set", workMd.getDateAdded());
        assertNotNull("Date updated must be set", workMd.getDateUpdated());

        assertEquals("Object title", workMd.getTitle());
        assertEquals("Boxy", workMd.getCreator().get(0));

        assertAncestorIds(workMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(workMd.getContentStatus());

        assertEquals(1, workMd.getDatastream().size());
        assertNotNull(workMd.getDatastreamObject(RepositoryPathConstants.ORIGINAL_FILE));

        assertTrue("Content type was not set to text", workMd.getContentType().get(0).contains("text"));

        assertTrue("Read groups did not contain assigned group", workMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", workMd.getAdminGroup().contains("admin"));

        assertEquals("Primary object not set", fileObj.getPid().getId(),
                workMd.getRelation(Cdr.primaryObject.getURI()).get(0));
    }

    @Test
    public void testIndexCollection() throws Exception {
        indexObjectsInTripleStore(rootObj, unitObj, collObj);

        setMessageTarget(collObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(collObj.getPid().getId(), accessGroups);
        BriefObjectMetadata collMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Collection", collMd.getResourceType());

        assertNotNull("Date added must be set", collMd.getDateAdded());
        assertNotNull("Date updated must be set", collMd.getDateUpdated());

        assertEquals(collObj.getPid().getId(), collMd.getTitle());

        assertAncestorIds(collMd, rootObj, unitObj, collObj);

        assertNotNull(collMd.getContentStatus());

        assertNull(collMd.getDatastream());

        assertNull(collMd.getContentType());

        assertTrue("Read groups did not contain assigned group", collMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", collMd.getAdminGroup().contains("admin"));
    }

    @Test
    public void testIndexFileObject() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        // Revoking patron access on the file
        Model fileModel = ModelFactory.createDefaultModel();
        Resource fileResc = fileModel.getResource("");
        fileResc.addProperty(CdrAcl.patronAccess, PatronAccess.none.name());
        String contentText = "Content";
        FileObject fileObj = workObj.addDataFile(new ByteArrayInputStream(contentText.getBytes()),
                "text.txt", "text/plain", null, null, fileModel);

        indexObjectsInTripleStore(rootObj, workObj, fileObj, unitObj, collObj);

        setMessageTarget(fileObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(fileObj.getPid().getId(), accessGroups);
        BriefObjectMetadata fileMd = solrSearchService.getObjectById(idRequest);

        assertEquals(ResourceType.File.name(), fileMd.getResourceType());

        assertNotNull("Date added must be set", fileMd.getDateAdded());
        assertNotNull("Date updated must be set", fileMd.getDateUpdated());

        assertEquals("text.txt", fileMd.getTitle());

        assertAncestorIds(fileMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(fileMd.getContentStatus());

        assertEquals(1, fileMd.getDatastream().size());
        assertNotNull(fileMd.getDatastreamObject(RepositoryPathConstants.ORIGINAL_FILE));

        assertTrue("Content type was not set to text", fileMd.getContentType().get(0).contains("text"));

        assertFalse("Read group should not be assigned", fileMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", fileMd.getAdminGroup().contains("admin"));
    }

    private void assertAncestorIds(BriefObjectMetadata md, RepositoryObject... ancestorObjs) {
        String joinedIds = "/" + Arrays.stream(ancestorObjs)
                .map(obj -> obj.getPid().getId())
                .collect(Collectors.joining("/"));

        assertEquals(joinedIds, md.getAncestorIds());
    }
}
