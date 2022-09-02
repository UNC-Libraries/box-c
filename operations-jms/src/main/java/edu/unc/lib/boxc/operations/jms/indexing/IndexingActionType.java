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
package edu.unc.lib.boxc.operations.jms.indexing;

import java.net.URI;
import java.net.URISyntaxException;

import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;

/**
 * Indexing actions
 * @author count0
 *
 */
public enum IndexingActionType {
    ADD("Add/Update", "Adds or updates the entry for the specified object"),
    UPDATE_DESCRIPTION("Update description", "Updates the descriptive metadata for the specified object"),
    UPDATE_DATASTREAMS("Update datastreams", "Updates datastream metadata for the specified object"),
    UPDATE_WORK_FILES("Update work files", "Set of files within a work has been updated."),
    UPDATE_FULL_TEXT("Update full text", "Updates the full text data for the specified object"),
    DELETE("Remove from Index", "Removes the index entry for the specified object"),
    COMMIT("Commit", "Causes an immediate upload and commit of pending updates"),
    RECURSIVE_ADD("Update Path",
            "Updates this pid and issues recursive add actions for all of its Fedora children"),
    RECURSIVE_REINDEX("In-place Reindex",
            "Performs a recursive reindex of this object and all its children "
            + "Updates this pid based off the originating structure, then cleans up any stale records."),
    DELETE_SOLR_TREE("Delete Tree from Index",
            "Deletes an object and all children that contained by it based on ancestorPath"),
    CLEAN_REINDEX("Clean Reindex", "Cleans out the path starting at the object specified and then reindexes it"),
    DELETE_CHILDREN_PRIOR_TO_TIMESTAMP("Cleanup Outdated Records",
            "Deletes the trees of all children of the starting node"
            + " if they have not been updated since the given timestamp"),
    CLEAR_INDEX("Delete Index", "Deletes everything from the index"),
    UPDATE_STATUS("Update status",
            "Partial update operation which refreshes the status of an object and all of its children"),
    UPDATE_PATH("Update Path", "Partial update which updates location related properties of an object"),
    UPDATE_PARENT_PATH_INFO("Update Parent Path Info",
            "Partial update to the path information of a single object"),
    UPDATE_PARENT_PATH_TREE("Update Parent Path Info for a tree of objects",
            "Partial update to the path information of children of the specified object"),
    MOVE("Move", "Partial update which updates the location and access control of an object and all its children"),
    UPDATE_ACCESS("Update access control",
            "Partial update which refreshes the access control for an object"),
    UPDATE_ACCESS_TREE("Update access control of a tree",
            "Partial update which refreshes the access control for an object and all its children"),
    ADD_SET_TO_PARENT("Add Set To Parent", "Indexes a set of newly added children contained by a shared parent"),
    UPDATE_TYPE("Update Resource Type", "Update the resource type of an object"),
    UPDATE_TYPE_TREE("Update Resource Type Tree",
            "Update the resource type of a set of objects and all their children"),
    SET_PRIMARY_OBJECT("Set Primary Object", "Update the primary object for a work"),
    UPDATE_MEMBER_ORDER("Update Member Order", "Update the order of members within an object"),
    UPDATE_MEMBER_ORDER_CHILD("Update Member Order Child", "Update the order of a container member"),
    UPDATE_MEMBER_ORDER_PARENT("Update Member Order Parent", "Update a container after its order changed"),
    UNKNOWN("Unknown action", "Unknown action");

    private final String label;
    private final String description;
    private URI uri;
    public static final String namespace = JDOMNamespaceUtil.CDR_MESSAGE_NS.getURI() + "/solr/";

    IndexingActionType(String label, String description) {
        this.label = label;
        this.description = description;
        try {
            this.uri = new URI(JDOMNamespaceUtil.CDR_MESSAGE_NS.getURI() + "/solr/" + name());
        } catch (URISyntaxException e) {
            Error x = new ExceptionInInitializerError("Error creating URI for SolrUpdateAction " + name());
            x.initCause(e);
            throw x;
        }
    }

    public String getName() {
        return this.name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public URI getURI() {
        return this.uri;
    }

    public boolean equals(String value) {
        return this.uri.toString().equals(value);
    }

    @Override
    public String toString() {
        return this.uri.toString();
    }

    /**
     * Finds an action that matches the full action uri provided.
     * @param value
     * @return
     */
    public static IndexingActionType getAction(String value) {
        if (value == null) {
            return null;
        }
        for (IndexingActionType action: values()) {
            if (action.equals(value)) {
                return action;
            }
        }
        return null;
    }
}