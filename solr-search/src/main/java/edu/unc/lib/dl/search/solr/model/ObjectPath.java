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

/**
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPath {

    private final List<ObjectPathEntry> entries;

    public ObjectPath(List<ObjectPathEntry> entries) {
        this.entries = entries;
    }

    public List<ObjectPathEntry> getEntries() {
        return entries;
    }

    public String getName(String pid) {
        ObjectPathEntry entry = getByPID(pid);
        return entry == null ? null : entry.getName();
    }

    private ObjectPathEntry getByPID(String pid) {
        for (ObjectPathEntry entry : entries) {
            if (entry.getPid().equals(pid)) {
                return entry;
            }
        }

        return null;
    }
}
