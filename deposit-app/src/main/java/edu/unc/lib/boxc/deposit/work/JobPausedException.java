package edu.unc.lib.boxc.deposit.work;

/**
 * Thrown if a deposit job is paused
 *
 * @author bbpennel
 */
public class JobPausedException extends JobInterruptedException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public JobPausedException(String message) {
        super(message);
    }

}
