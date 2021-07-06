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
package edu.unc.lib.dl.data.ingest.solr.action;

import static edu.unc.lib.dl.util.IndexingActionType.CLEAN_REINDEX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * @author bbpennel
 */
public class IndexTreeCleanActionTest {
    private static final String USER = "user";

    @Mock
    private SolrUpdateDriver driver;
    @Mock
    private DocumentIndexingPipeline pipeline;
    @Mock
    private DeleteSolrTreeAction deleteAction;
    @Mock
    private SolrUpdateRequest request;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;

    private RecursiveTreeIndexer treeIndexer;

    @Mock
    private ContentContainerObject containerObj;

    @Captor
    private ArgumentCaptor<PID> pidCaptor;

    private PID pid;

    private IndexTreeCleanAction action;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        when(request.getPid()).thenReturn(pid);

        treeIndexer = new RecursiveTreeIndexer();
        treeIndexer.setIndexingMessageSender(messageSender);

        action = new IndexTreeCleanAction();
        action.setDeleteAction(deleteAction);
        action.setTreeIndexer(treeIndexer);
        action.setSolrUpdateDriver(driver);
        action.setActionType(IndexingActionType.ADD.name());
        action.setRepositoryObjectLoader(repositoryObjectLoader);

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(containerObj);
        when(containerObj.getPid()).thenReturn(pid);
        Model model = ModelFactory.createDefaultModel();
        when(containerObj.getResource()).thenReturn(model.getResource(pid.getRepositoryPath()));
    }

    @Test
    public void testPerformAction() throws Exception {
        request = new SolrUpdateRequest(pid.getRepositoryPath(), CLEAN_REINDEX, "1", USER);

        action.performAction(request);

        verify(deleteAction).performAction(any(SolrUpdateRequest.class));
        verify(driver).commit();

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));
        assertEquals(pid, pidCaptor.getValue());
    }
}
