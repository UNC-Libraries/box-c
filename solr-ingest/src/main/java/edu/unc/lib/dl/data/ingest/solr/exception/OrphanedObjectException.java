package edu.unc.lib.dl.data.ingest.solr.exception;

public class OrphanedObjectException extends IndexingException {
	private static final long serialVersionUID = 1L;

	public OrphanedObjectException(String message) {
		super(message);
	}
}
