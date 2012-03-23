package edu.unc.lib.dl.update;

public class UpdateException extends Exception {
	private static final long serialVersionUID = -3160336020986091222L;

	public UpdateException(String msg) {
		super(msg);
	}

	public UpdateException(String msg, Throwable e) {
		super(msg, e);
	}
}
