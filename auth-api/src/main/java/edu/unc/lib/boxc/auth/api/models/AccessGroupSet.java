package edu.unc.lib.boxc.auth.api.models;

import java.util.Set;

/**
 * Storage class for a list of access groups related to a single entity.
 * @author bbpennel
 */
public interface AccessGroupSet extends Set<String> {

    void addAccessGroup(String group);

    String joinAccessGroups(String delimiter);

    String joinAccessGroups(String delimiter, String prefix, boolean escapeColons);

}