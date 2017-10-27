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
package edu.unc.lib.dl.acl.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Storage class for a list of access groups related to a single entity.
 * @author bbpennel
 */
public class AccessGroupSet extends HashSet<String> {
    private static final long serialVersionUID = 1L;

    public AccessGroupSet() {
        super();
    }

    public AccessGroupSet(String groups) {
        super();
        addAccessGroups(groups.split(";"));
    }

    public AccessGroupSet(String[] groups) {
        super();
        addAccessGroups(groups);
    }

    public AccessGroupSet(AccessGroupSet groups) {
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

    public void addAccessGroup(String group) {
        if (group == null) {
            return;
        }
        this.add(group);
    }

    /**
     * Determines is any of the objects contained within the specified
     * collection are present in the access group set. If the objects contain
     * pid prefixes, they are stripped off before checking
     *
     * @param c collection to be checked for matches.
     * @return true if this collection contains any objects from the specified
     *         collection
     */
    public static boolean containsAny(AccessGroupSet accessGroupSet, Collection<String> c) {
        if (c == null || c.size() == 0 || accessGroupSet.size() == 0) {
            return false;
        }
        Iterator<String> cIt = c.iterator();
        String nextKey;
        int pidDelimiter;
        while (cIt.hasNext()) {
            nextKey = cIt.next();
            pidDelimiter = nextKey.lastIndexOf('/');
            if (pidDelimiter > -1) {
                nextKey = nextKey.substring(pidDelimiter + 1);
            }
            if (accessGroupSet.contains(nextKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAny(Collection<String> c) {
        return containsAny(this, c);
    }

    public String joinAccessGroups(String delimiter) {
        return this.joinAccessGroups(delimiter, null, false);
    }

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
