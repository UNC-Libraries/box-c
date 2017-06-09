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

/**
 * Request object for a single ID along with access restrictions and requested result data.
 * @author bbpennel
 */
public class SimpleIdRequest {
    protected String id;
    protected List<String> resultFields;
    protected AccessGroupSet accessGroups;

    public SimpleIdRequest(String id) {
        this.id = id;
    }

    public SimpleIdRequest(List<String> resultFields, AccessGroupSet accessGroups) {
        this.accessGroups = accessGroups;
        this.resultFields = resultFields;
    }

    public SimpleIdRequest(String id, List<String> resultFields) {
        this.id = id;
        this.resultFields = resultFields;
    }

    public SimpleIdRequest(String id, List<String> resultFields, AccessGroupSet accessGroups) {
        this.id = id;
        this.accessGroups = accessGroups;
        this.resultFields = resultFields;
    }

    public SimpleIdRequest(String id, AccessGroupSet accessGroups) {
        this(id, null, accessGroups);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AccessGroupSet getAccessGroups() {
        return accessGroups;
    }

    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.accessGroups = accessGroups;
    }

    public List<String> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }
}
