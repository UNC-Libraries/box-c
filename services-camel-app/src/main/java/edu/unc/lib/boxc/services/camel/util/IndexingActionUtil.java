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
package edu.unc.lib.boxc.services.camel.util;

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
                    IndexingActionType.COMMIT,
                    IndexingActionType.DELETE);
}
