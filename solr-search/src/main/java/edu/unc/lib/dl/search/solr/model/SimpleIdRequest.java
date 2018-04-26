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
package edu.unc.lib.dl.search.solr.model;

import java.util.List;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * Request object for a single ID along with access restrictions and requested result data.
 * @author bbpennel
 */
public class SimpleIdRequest {
    protected final PID pid;
    protected List<String> resultFields;
    protected final AccessGroupSet accessGroups;

    public SimpleIdRequest(String id, AccessGroupSet accessGroups) {
        this(id, null, accessGroups);
    }

    public SimpleIdRequest(PID pid, AccessGroupSet accessGroups) {
        this(pid, null, accessGroups);
    }

    public SimpleIdRequest(String id, List<String> resultFields, AccessGroupSet accessGroups) {
        this(PIDs.get(id), resultFields, accessGroups);
    }

    public SimpleIdRequest(PID pid, List<String> resultFields, AccessGroupSet accessGroups) {
        this.pid = pid;
        this.accessGroups = accessGroups;
        this.resultFields = resultFields;
    }

    /**
     * @return the pid
     */
    public PID getPid() {
        return pid;
    }

    public String getId() {
        return pid.getId();
    }

    public AccessGroupSet getAccessGroups() {
        return accessGroups;
    }

    public List<String> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }
}
