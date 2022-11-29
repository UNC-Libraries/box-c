package edu.unc.lib.boxc.indexing.solr.exception;

/**
 * Exception while attempting to index to solr
 *
 * @author bbpennel
 */
public class IndexingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String body;

    public IndexingException(String message, Throwable cause, String body) {
        super(message, cause);
        this.body = body;
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingException(String message) {
        super(message);
    }

    public String getBody() {
        return body;
    }
}
