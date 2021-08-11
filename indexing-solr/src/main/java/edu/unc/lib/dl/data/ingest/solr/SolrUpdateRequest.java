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

import java.util.Map;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 * Represents a request to update an object identified by pid.
 * @author bbpennel
 */
public class SolrUpdateRequest extends UpdateNodeRequest {
    private static final long serialVersionUID = 1L;
    protected PID pid;
    protected String targetLabel;
    protected IndexingActionType action;
    protected Map<String, String> params;

    public SolrUpdateRequest(String pid, IndexingActionType action) {
        this(pid, action, null, null);
    }

    public SolrUpdateRequest(String pid, IndexingActionType action, String messageID, String userID) {
        super(messageID);
        if (pid == null || action == null) {
            throw new IllegalArgumentException("Both a target pid and an action are required.");
        }
        this.pid = PIDs.get(pid);
        this.action = action;
        this.userID = userID;
    }

    public PID getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = PIDs.get(pid);
    }

    public IndexingActionType getUpdateAction() {
        return action;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    @Override
    public String getTargetID() {
        return pid.getId();
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }

    @Override
    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    @Override
    public String getAction() {
        return this.action.getName();
    }

    @Override
    public String getNamespace() {
        return IndexingActionType.namespace;
    }

    @Override
    public String getQualifiedAction() {
        return this.action.getURI().toString();
    }

    @Override
    public long getTimeCreated() {
        return timeCreated;
    }

    /**
     * @return the params
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
