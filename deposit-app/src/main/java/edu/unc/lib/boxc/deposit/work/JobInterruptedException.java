package edu.unc.lib.boxc.deposit.work;

/**
 * @author bbpennel
 * @date Aug 4, 2014
 */
public class JobInterruptedException extends RuntimeException {

    private static final long serialVersionUID = 718488999114016706L;

    public JobInterruptedException(String message) {
        super(message);
    }

    public JobInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
