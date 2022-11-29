package edu.unc.lib.boxc.search.solr.models;

import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.api.models.ObjectPathEntry;

/**
 * A hierarchy path for a content object
 *
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPathImpl implements ObjectPath {

    private final List<ObjectPathEntry> entries;

    public ObjectPathImpl(List<ObjectPathEntry> entries) {
        this.entries = entries;
    }

    @Override
    public List<ObjectPathEntry> getEntries() {
        return entries;
    }

    @Override
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

    /**
     * Return a string representation of the path constructed from the names of
     * entries in this path
     *
     * @return name path
     */
    @Override
    public String toNamePath() {
        return "/" + entries.stream().map(e -> e.getName().replaceAll("/", "\\/"))
                .collect(Collectors.joining("/"));
    }

    /**
     * Return a string representation of the path constructed from the ids of
     * entries in this path.
     *
     * @return id path
     */
    @Override
    public String toIdPath() {
        return "/" + entries.stream().map(e -> e.getPid())
                .collect(Collectors.joining("/"));
    }
}
