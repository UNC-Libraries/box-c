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
package edu.unc.lib.dl.data.ingest.solr;

import java.util.concurrent.atomic.AtomicInteger;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.message.ActionMessage;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateNodeRequest implements ActionMessage {
    private static final long serialVersionUID = 1L;

    protected String messageID;
    protected long timeCreated = System.currentTimeMillis();
    protected long timeStarted;
    protected long timeFinished;

    protected DocumentIndexingPackage documentIndexingPackage;

    protected ProcessingStatus status;
    protected AtomicInteger childrenPending;
    protected AtomicInteger childrenProcessed;

    public UpdateNodeRequest(String messageID) {
        this.messageID = messageID;

        childrenPending = new AtomicInteger(0);
        childrenProcessed = new AtomicInteger(0);
    }

    public UpdateNodeRequest(String messageID, ProcessingStatus status) {
        this(messageID);
        this.status = status;
    }

    public void requestCompleted() {
        timeFinished = System.currentTimeMillis();
        this.cleanupExternalReferences();

        if (ProcessingStatus.ACTIVE.equals(this.status)) {
            this.status = ProcessingStatus.FINISHED;
        }
    }

    /**
     * Cleans up or allows for cleanup of references to external resources that are no longer needed after
     * this message has finished being processed, but is still being retained.
     */
    protected void cleanupExternalReferences() {
        this.documentIndexingPackage = null;
    }

    public void setChildrenPending(int newValue) {
        this.childrenPending.set(newValue);
    }

    public int getChildrenPending() {
        return childrenPending.get();
    }

    public int incrementChildrenProcessed() {
        return this.childrenProcessed.incrementAndGet();
    }

    public int getChildrenProcessed() {
        return childrenProcessed.get();
    }

    @Override
    public String getMessageID() {
        return messageID;
    }

    @Override
    public String getTargetID() {
        return messageID;
    }

    @Override
    public String getTargetLabel() {
        return messageID;
    }

    @Override
    public void setTargetLabel(String targetLabel) {
    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getQualifiedAction() {
        return null;
    }

    @Override
    public long getTimeCreated() {
        return this.timeCreated;
    }

    public long getTimeFinished() {
        return timeFinished;
    }

    public long getTimeStarted() {
        return this.timeStarted;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
        if (this.status == ProcessingStatus.FINISHED || this.status == ProcessingStatus.FAILED) {
            this.timeFinished = System.currentTimeMillis();
        }
        if (this.status == ProcessingStatus.ACTIVE) {
            this.timeStarted = System.currentTimeMillis();
        }
    }

    public DocumentIndexingPackage getDocumentIndexingPackage() {
        return documentIndexingPackage;
    }

    public void setDocumentIndexingPackage(DocumentIndexingPackage documentIndexingPackage) {
        this.documentIndexingPackage = documentIndexingPackage;
    }
}
