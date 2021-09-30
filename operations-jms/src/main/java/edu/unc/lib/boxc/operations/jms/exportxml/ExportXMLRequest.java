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
package edu.unc.lib.boxc.operations.jms.exportxml;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * A request object for a bulk XML export
 *
 * @author bbpennel
 */
public class ExportXMLRequest {
    private List<String> pids;
    private boolean exportChildren;
    private String email;
    private Instant requestedTimestamp;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private boolean excludeNoDatastreams;

    public ExportXMLRequest() {
    }

    public List<String> getPids() {
        return pids;
    }

    public void setPids(List<String> pids) {
        this.pids = pids;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean getExportChildren() {
        return exportChildren;
    }

    public void setExportChildren(boolean exportChildren) {
        this.exportChildren = exportChildren;
    }

    /**
     * @return Time at which this request was generated
     */
    public Instant getRequestedTimestamp() {
        return requestedTimestamp;
    }

    public void setRequestedTimestamp(Instant requestedTimestamp) {
        this.requestedTimestamp = requestedTimestamp;
    }

    /**
     * @return The agent making this request
     */
    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    /**
     * @return True if objects which return no datastreams should be excluded from the export
     */
    public boolean getExcludeNoDatastreams() {
        return excludeNoDatastreams;
    }

    public void setExcludeNoDatastreams(boolean excludeNoDatastreams) {
        this.excludeNoDatastreams = excludeNoDatastreams;
    }
}
