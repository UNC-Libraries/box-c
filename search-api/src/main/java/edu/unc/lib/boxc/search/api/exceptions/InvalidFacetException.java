package edu.unc.lib.boxc.search.api.exceptions;

/**
 * 
 * @author bbpennel
 *
 */
public class InvalidFacetException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidFacetException(String msg, Exception e) {
        super(msg + "\n" + e.getMessage());
        this.setStackTrace(e.getStackTrace());
    }

    public InvalidFacetException(String msg) {
        super(msg);
    }
}
