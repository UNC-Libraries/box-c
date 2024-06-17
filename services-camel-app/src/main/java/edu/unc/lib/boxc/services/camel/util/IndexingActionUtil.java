package edu.unc.lib.boxc.services.camel.util;

import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

import java.util.EnumSet;
import java.util.Set;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.CLEAN_REINDEX;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_ADD;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_REINDEX;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_ACCESS_TREE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_PARENT_PATH_TREE;

/**
 * Utilities related to solr Indexing Actions
 *
 * @author bbpennel
 */
public class IndexingActionUtil {
    private IndexingActionUtil() {
    }

    // Indexing actions which involve numerous objects
    public static final Set<IndexingActionType> LARGE_ACTIONS =
            EnumSet.of(RECURSIVE_REINDEX,
                    RECURSIVE_ADD,
                    CLEAN_REINDEX,
                    DELETE_SOLR_TREE,
                    IndexingActionType.MOVE,
                    ADD_SET_TO_PARENT,
                    UPDATE_ACCESS_TREE,
                    DELETE_CHILDREN_PRIOR_TO_TIMESTAMP,
                    IndexingActionType.UPDATE_MEMBER_ORDER,
                    UPDATE_PARENT_PATH_TREE);

    // Indexing actions which impact one object, or no objects directly
    public static final Set<IndexingActionType> SMALL_ACTIONS =
            EnumSet.of(IndexingActionType.ADD,
                    IndexingActionType.UPDATE_DESCRIPTION,
                    IndexingActionType.UPDATE_ACCESS,
                    IndexingActionType.UPDATE_PATH,
                    IndexingActionType.UPDATE_DATASTREAMS,
                    IndexingActionType.UPDATE_WORK_FILES,
                    IndexingActionType.UPDATE_FULL_TEXT,
                    IndexingActionType.UPDATE_PARENT_PATH_INFO,
                    IndexingActionType.UPDATE_MEMBER_ORDER_CHILD,
                    IndexingActionType.UPDATE_MEMBER_ORDER_PARENT,
                    IndexingActionType.UPDATE_VIEW_BEHAVIOR,
                    IndexingActionType.UPDATE_STREAMING_URL,
                    IndexingActionType.COMMIT,
                    IndexingActionType.DELETE);
}
