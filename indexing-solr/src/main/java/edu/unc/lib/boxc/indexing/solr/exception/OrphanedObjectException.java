package edu.unc.lib.boxc.indexing.solr.exception;

/**
 * 
 * @author bbpennel
 *
 */
public class OrphanedObjectException extends IndexingException {
    private static final long serialVersionUID = 1L;

    public OrphanedObjectException(String message) {
        super(message);
    }
}
