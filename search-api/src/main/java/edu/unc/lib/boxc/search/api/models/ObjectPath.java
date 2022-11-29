package edu.unc.lib.boxc.search.api.models;

import java.util.List;

/**
 * A hierarchy path for a content object
 * @author bbpennel
 */
public interface ObjectPath {

    /**
     * @return All entries in this object path
     */
    List<ObjectPathEntry> getEntries();

    /**
     * @param pid
     * @return The title/name of the object in this path with the provided pid
     */
    String getName(String pid);

    /**
     * Return a string representation of the path constructed from the names of
     * entries in this path
     *
     * @return name path
     */
    String toNamePath();

    /**
     * Return a string representation of the path constructed from the ids of
     * entries in this path.
     *
     * @return id path
     */
    String toIdPath();

}