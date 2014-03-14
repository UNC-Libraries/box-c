package edu.unc.lib.deposit.work;

public class JobFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2094423835072435563L;

	public JobFailedException(String message) {
		super(message);
	}
	
	public JobFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
