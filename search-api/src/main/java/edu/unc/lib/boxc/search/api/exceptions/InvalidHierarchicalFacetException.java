package edu.unc.lib.boxc.search.api.exceptions;

/**
 * 
 * @author bbpennel
 *
 */
public class InvalidHierarchicalFacetException extends InvalidFacetException {
    private static final long serialVersionUID = 1L;

    public InvalidHierarchicalFacetException(String msg, Exception e) {
        super(msg);
        this.setStackTrace(e.getStackTrace());
    }

    public InvalidHierarchicalFacetException(String msg) {
        super(msg);
    }
}
