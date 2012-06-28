package edu.unc.lib.dl.data.ingest.solr;

public enum ProcessingStatus {
	ACTIVE("active"), INPROGRESS("inprogress"), BLOCKED("blocked"), QUEUED("queued"), FINISHED("finished"), FAILED("failed");
	
	String name;
	
	ProcessingStatus(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
