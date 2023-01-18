package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_ADD;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexTreeInplaceAction;
import edu.unc.lib.boxc.indexing.solr.action.UpdateTreeAction;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
    @BeforeEach
    public void setupInplace() throws Exception {

        action.setAccessGroups(new AccessGroupSetImpl("admin"));
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
