package edu.unc.lib.boxc.search.api.exceptions;

/**
 * Runtime exception originating from a solr interaction
 *
 * @author bbpennel
 *
 */
public class SolrRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 5827878385587178461L;

    public SolrRuntimeException() {
    }

    public SolrRuntimeException(String message) {
        super(message);
    }

    public SolrRuntimeException(Throwable cause) {
        super(cause);
    }

    public SolrRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
