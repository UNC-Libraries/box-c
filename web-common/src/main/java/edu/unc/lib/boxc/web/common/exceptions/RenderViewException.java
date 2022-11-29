package edu.unc.lib.boxc.web.common.exceptions;

/**
 * 
 * @author bbpennel
 */
public class RenderViewException extends Exception {
    private static final long serialVersionUID = -6741051042605136668L;

    public RenderViewException() {
        super();
    }

    public RenderViewException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public RenderViewException(String arg0) {
        super(arg0);
    }

    public RenderViewException(Throwable arg0) {
        super(arg0);
    }
}
