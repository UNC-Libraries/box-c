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

import static edu.unc.lib.dl.util.IndexingActionType.RECURSIVE_ADD;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.auth.fcrepo.model.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
    @Before
    public void setupInplace() throws Exception {

        action.setAccessGroups(new AccessGroupSet("admin"));
        ((IndexTreeInplaceAction) action).setIndexingMessageSender(messageSender);
    }

    @Override
    protected UpdateTreeAction getAction() {
        return new IndexTreeInplaceAction();
    }

    @Test
    public void testIndexingWithCleanup() throws Exception {
        SolrUpdateRequest request = new SolrUpdateRequest(corpus.pid2.getRepositoryPath(),
                RECURSIVE_ADD, "1", USER);
        action.performAction(request);

        verify(messageSender, times(3)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP));
    }
}
