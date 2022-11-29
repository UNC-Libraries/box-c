package edu.unc.lib.boxc.indexing.solr.exception;

/**
 * 
 * @author bbpennel
 *
 */
public class ObjectTombstonedException extends IndexingException {
    private static final long serialVersionUID = 1L;

    public ObjectTombstonedException(String message) {
        super(message);
    }

}
