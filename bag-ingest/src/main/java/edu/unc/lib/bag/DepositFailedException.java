package edu.unc.lib.bag;

public class DepositFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2094423835072435563L;

	public DepositFailedException(String message) {
		super(message);
	}
	
	public DepositFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
