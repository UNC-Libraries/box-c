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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.Tombstone;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Performs depth first indexing of a tree of repository objects, starting at the PID of the provided update request.
 *
 * @author bbpennel
 *
 */
public class RecursiveTreeIndexer {
    private static final Logger log = LoggerFactory.getLogger(RecursiveTreeIndexer.class);

    private IndexingMessageSender messageSender;

    public RecursiveTreeIndexer() {
    }

    public void index(RepositoryObject repoObj, IndexingActionType actionType, String userid) throws IndexingException {
        PID pid = repoObj.getPid();

        messageSender.sendIndexingOperation(userid, pid, actionType);

        // Start indexing the children
        if (repoObj instanceof ContentContainerObject) {
            indexChildren((ContentContainerObject) repoObj, actionType, userid);
        }
    }

    public void indexChildren(ContentContainerObject parent, IndexingActionType actionType, String userid)
            throws IndexingException {
        List<ContentObject> children = parent.getMembers();
        if (children == null || children.size() == 0) {
            return;
        }
        log.debug("Queuing {} children of {} for indexing", children.size(), parent.getPid());
        for (ContentObject child : children) {
            if (!(child instanceof Tombstone)) {
                this.index(child, actionType, userid);
            }
        }
    }

    /**
     * @param messageSender the messageSender to set
     */
    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }
}