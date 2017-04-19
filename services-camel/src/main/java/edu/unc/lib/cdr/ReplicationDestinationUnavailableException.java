package edu.unc.lib.cdr;

@SuppressWarnings("serial")
public class ReplicationDestinationUnavailableException extends Exception {
	public ReplicationDestinationUnavailableException() {}
	
	public ReplicationDestinationUnavailableException(String message) {
		super(message);
	}
}
