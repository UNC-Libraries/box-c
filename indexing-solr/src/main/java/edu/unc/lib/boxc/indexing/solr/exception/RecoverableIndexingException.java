package edu.unc.lib.boxc.indexing.solr.exception;

/**
 * Indexing exception which it may be possible to recover from
 *
 * @author bbpennel
 */
public class RecoverableIndexingException extends IndexingException {

    public RecoverableIndexingException(String message, Throwable cause, String body) {
        super(message, cause, body);
    }

    public RecoverableIndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoverableIndexingException(String message) {
        super(message);
    }
}
