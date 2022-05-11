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
package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.services.camel.solr.SolrIngestProcessor;

/**
 *
 * @author bbpennel
 *
 */
public class SolrIngestProcessorIT extends AbstractSolrProcessorIT {

    private SolrIngestProcessor processor;

    private static final String CONTENT_TEXT = "Content";
    private static final String TEXT_EXTRACT = "Cone Tent";

    @Autowired
    private DocumentIndexingPipeline solrFullUpdatePipeline;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private DerivativeService derivativeService;
    @Mock
    private AgentPrincipals agent;
    @Autowired
    private MessageSender updateWorkSender;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        processor = new SolrIngestProcessor(dipFactory, solrFullUpdatePipeline, driver, repositoryObjectLoader);
        processor.setUpdateWorkSender(updateWorkSender);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @Test
    public void testIndexDescribedWork() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());
        InputStream modsStream = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workObj.getPid(), modsStream));

        indexObjectsInTripleStore();

        setMessageTarget(fileObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Cdr.FileObject.getURI());
        processor.process(exchange);
        server.commit();

        setMessageTarget(workObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid(), accessGroups);
        ContentObjectRecord workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertNotNull("Date added must be set", workMd.getDateAdded());
        assertNotNull("Date updated must be set", workMd.getDateUpdated());

        assertEquals("Object title", workMd.getTitle());
        assertEquals("Boxy", workMd.getCreator().get(0));

        assertAncestorIds(workMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(workMd.getContentStatus());

        assertEquals(2, workMd.getDatastream().size());
        assertNotNull(workMd.getDatastreamObject(ORIGINAL_FILE.getId()));
        assertNotNull(workMd.getDatastreamObject(MD_DESCRIPTIVE.getId()));

        assertTrue("Content type was not set to text", workMd.getFileFormatCategory().get(0).contains("Text"));

        assertTrue("Read groups did not contain assigned group", workMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", workMd.getAdminGroup().contains("admin"));
    }

    @Test
    public void testIndexCollection() throws Exception {
        indexObjectsInTripleStore();
        repositoryObjectSolrIndexer.index(unitObj.getPid());

        setMessageTarget(collObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(collObj.getPid(), accessGroups);
        ContentObjectRecord collMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Collection", collMd.getResourceType());

        assertNotNull("Date added must be set", collMd.getDateAdded());
        assertNotNull("Date updated must be set", collMd.getDateUpdated());

        assertEquals(collObj.getPid().getId(), collMd.getTitle());

        assertAncestorIds(collMd, rootObj, unitObj, collObj);

        assertNotNull(collMd.getContentStatus());

        assertNull(collMd.getDatastream());

        assertTrue(CollectionUtils.isEmpty(collMd.getFileFormatCategory()));

        assertTrue("Read groups did not contain assigned group", collMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", collMd.getAdminGroup().contains("admin"));
    }

    @Test
    public void testIndexFileObject() throws Exception {
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        // Revoking patron access on the file
        Model fileModel = ModelFactory.createDefaultModel();
        Resource fileResc = fileModel.getResource("");
        fileResc.addProperty(CdrAcl.none, AUTHENTICATED_PRINC);
        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null, fileModel);

        Path derivPath = derivativeService.getDerivativePath(fileObj.getPid(), DatastreamType.FULLTEXT_EXTRACTION);
        FileUtils.writeStringToFile(derivPath.toFile(), TEXT_EXTRACT, UTF_8);

        indexObjectsInTripleStore();

        setMessageTarget(fileObj);
        processor.process(exchange);
        server.commit();

        List<String> allFields = Arrays.stream(SearchFieldKey.values())
                .map(SearchFieldKey::name).collect(Collectors.toList());
        SimpleIdRequest idRequest = new SimpleIdRequest(fileObj.getPid(), allFields, accessGroups);
        ContentObjectRecord fileMd = solrSearchService.getObjectById(idRequest);

        assertEquals(ResourceType.File.name(), fileMd.getResourceType());

        assertNotNull("Date added must be set", fileMd.getDateAdded());
        assertNotNull("Date updated must be set", fileMd.getDateUpdated());

        assertEquals("text.txt", fileMd.getTitle());

        assertAncestorIds(fileMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(fileMd.getContentStatus());

        assertEquals(2, fileMd.getDatastream().size());
        assertNotNull(fileMd.getDatastreamObject(ORIGINAL_FILE.getId()));
        assertNotNull(fileMd.getDatastreamObject(DatastreamType.FULLTEXT_EXTRACTION.getId()));

        assertTrue("Content type was not set to text:" + fileMd.getFileFormatCategory(),
                fileMd.getFileFormatCategory().get(0).contains("Text"));

        assertFalse("Read group should not be assigned", fileMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", fileMd.getAdminGroup().contains("admin"));

        assertEquals(TEXT_EXTRACT, fileMd.getFullText());
    }

    @Test
    public void testIndexBinaryInWork() throws Exception {
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());

        BinaryObject binObj = fileObj.getOriginalFile();

        indexObjectsInTripleStore();

        setMessageTarget(binObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Fcrepo4Repository.Binary.getURI());
        processor.process(exchange);
        server.commit();

        setMessageTarget(workObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Cdr.Work.getURI());
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid(), accessGroups);
        ContentObjectRecord workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertEquals(1, workMd.getDatastream().size());
        assertNotNull(workMd.getDatastreamObject(ORIGINAL_FILE.getId()));

        assertTrue("Content type was not set to text", workMd.getFileFormatCategory().get(0).contains("Text"));

        idRequest = new SimpleIdRequest(fileObj.getPid(), accessGroups);
        ContentObjectRecord fileMd = solrSearchService.getObjectById(idRequest);

        assertEquals(ResourceType.File.name(), fileMd.getResourceType());

        assertEquals("text.txt", fileMd.getTitle());

        assertEquals(1, fileMd.getDatastream().size());
        assertNotNull(fileMd.getDatastreamObject(ORIGINAL_FILE.getId()));
    }

    private void assertAncestorIds(ContentObjectRecord md, RepositoryObject... ancestorObjs) {
        String joinedIds = "/" + Arrays.stream(ancestorObjs)
                .map(obj -> obj.getPid().getId())
                .collect(Collectors.joining("/"));

        assertEquals(joinedIds, md.getAncestorIds());
    }
}
