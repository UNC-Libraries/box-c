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
package edu.unc.lib.boxc.auth.fcrepo.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;

/**
 * Storage class for a list of access groups related to a single entity.
 * @author bbpennel
 */
public class AccessGroupSetImpl extends HashSet<String> implements AccessGroupSet {
    private static final long serialVersionUID = 1L;

    public AccessGroupSetImpl() {
        super();
    }

    public AccessGroupSetImpl(String groups) {
        super();
        addAccessGroups(groups.split(";"));
    }

    public AccessGroupSetImpl(String... groups) {
        super();
        addAccessGroups(groups);
    }

    public AccessGroupSetImpl(Collection<String> groups) {
        super(groups);
    }

    public void addAccessGroups(String[] groups) {
        if (groups == null) {
            return;
        }
        for (String group: groups) {
            if (group != null && group.length() > 0) {
                this.add(group);
            }
        }
    }

    @Override
    public void addAccessGroup(String group) {
        if (group == null) {
            return;
        }
        this.add(group);
    }

    @Override
    public String joinAccessGroups(String delimiter) {
        return this.joinAccessGroups(delimiter, null, false);
    }

    @Override
    public String joinAccessGroups(String delimiter, String prefix, boolean escapeColons) {
        StringBuffer sb = new StringBuffer();
        String value;
        boolean firstEntry = true;
        Iterator<String> agIt = this.iterator();
        while (agIt.hasNext()) {
            value = agIt.next();
            if (prefix != null) {
                value = prefix + value;
            }
            if (escapeColons) {
                value = value.replaceAll("\\:", "\\\\:");
            }
            if (firstEntry) {
                firstEntry = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(value);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return this.joinAccessGroups(" ", "", true);
    }
}
