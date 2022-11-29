package edu.unc.lib.boxc.search.api.models;

/**
 * A single tier entry in the content path of an object
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPathEntry {

    private String pid;
    private String name;
    private boolean isContainer;
    private String collectionId;

    public ObjectPathEntry(String pid, String name, boolean isContainer, String collectionId) {
        this.pid = pid;
        this.name = name;
        this.isContainer = isContainer;
        this.collectionId = collectionId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setContainer(boolean isContainer) {
        this.isContainer = isContainer;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String id) {
        this.collectionId = id;
    }

}
