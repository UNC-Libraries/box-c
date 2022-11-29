package edu.unc.lib.boxc.web.common.exceptions;

import java.io.IOException;

/**
 * 
 * @author count0
 *
 */
public class ClientAbortException extends IOException {
    private static final long serialVersionUID = 1L;

    public ClientAbortException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ClientAbortException(String arg0) {
        super(arg0);
    }

    public ClientAbortException(Throwable arg0) {
        super(arg0);
    }
}
