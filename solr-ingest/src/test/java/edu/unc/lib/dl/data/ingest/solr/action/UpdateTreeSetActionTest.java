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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateTreeSetActionTest {
    private static final String USER = "user";

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;
    @Captor
    private ArgumentCaptor<PID> pidCaptor;

    private ChildSetRequest request;

    protected UpdateTreeSetAction action;

    private RecursiveTreeIndexer treeIndexer;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        treeIndexer = new RecursiveTreeIndexer();
        treeIndexer.setIndexingMessageSender(messageSender);

        action = new UpdateTreeSetAction();
        action.setRepositoryObjectLoader(repositoryObjectLoader);
        action.setTreeIndexer(treeIndexer);
        action.setActionType(IndexingActionType.ADD.name());
    }

    @Test
    public void testSingleEmptyChild() throws Exception {
        ContentContainerObject containerObj = makeContainer();
        PID containerPid = containerObj.getPid();

        request = new ChildSetRequest(containerPid.getRepositoryPath(), asList(containerPid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(containerPid));
    }

    /**
     * Verify that all children included in the request are indexed
     */
    @Test
    public void testMultipleChildren() throws Exception {
        ContentContainerObject container1Obj = makeContainer();
        PID container1Pid = container1Obj.getPid();
        ContentContainerObject container2Obj = makeContainer();
        PID container2Pid = container2Obj.getPid();

        request = new ChildSetRequest(container1Pid.getRepositoryPath(),
                asList(container1Pid.getRepositoryPath(), container2Pid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender, times(2)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(container1Pid));
        assertTrue(pids.contains(container2Pid));
    }

    /**
     * Verify that children of the submitted children are indexed
     */
    @Test
    public void testNestedChildren() throws Exception {
        ContentContainerObject containerObj = makeContainer();
        PID containerPid = containerObj.getPid();
        ContentContainerObject childObj = addContainerToParent(containerObj);

        request = new ChildSetRequest(containerPid.getRepositoryPath(), asList(containerPid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender, times(2)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(containerPid));
        assertTrue(pids.contains(childObj.getPid()));
    }

    @Test(expected = IndexingException.class)
    public void testNotChildSetRequest() throws Exception {
        SolrUpdateRequest request = mock(SolrUpdateRequest.class);

        action.performAction(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoChildrenRequest() throws Exception {
        request = new ChildSetRequest(null, asList(),
                IndexingActionType.ADD, USER);

        action.performAction(request);
    }

    protected ContentContainerObject makeContainer() {
        PID pid = makePid();
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getMembers()).thenReturn(new ArrayList<>());
        when(container.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);

        return container;
    }

    protected ContentContainerObject addContainerToParent(ContentContainerObject container) {
        ContentContainerObject memberObj = makeContainer();
        container.getMembers().add(memberObj);
        return memberObj;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
