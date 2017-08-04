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

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class ChildSetRequest extends SolrUpdateRequest {
    private static final long serialVersionUID = 1L;
    private List<PID> children;

    public ChildSetRequest(String newParent, List<String> children, IndexingActionType action) {
        super(newParent, action);
        this.children = new ArrayList<PID>(children.size());
        for (String childString : children) {
            this.children.add(PIDs.get(childString));
        }
    }

    public List<PID> getChildren() {
        return children;
    }

    public void setChlidren(List<PID> children) {
        this.children = children;
    }
}
