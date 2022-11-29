package edu.unc.lib.boxc.indexing.solr;

/**
 * 
 * @author bbpennel
 *
 */
public enum ProcessingStatus {
    ACTIVE("active"), INPROGRESS("inprogress"), BLOCKED("blocked"),
    QUEUED("queued"), FINISHED("finished"), FAILED("failed"), IGNORED("ignored");

    String name;

    ProcessingStatus(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
