package edu.unc.lib.deposit.work;

/**
 * Thrown whenever the deposit cannot be completed for a given reason.
 * @author count0
 *
 */
public class DepositFailedException extends Throwable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4818301461775253637L;

	public DepositFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DepositFailedException(String message) {
		super(message);
	}

	public DepositFailedException(Throwable cause) {
		super(cause);
	}

}
