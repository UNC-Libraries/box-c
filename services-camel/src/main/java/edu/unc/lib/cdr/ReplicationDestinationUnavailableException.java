package edu.unc.lib.cdr;

@SuppressWarnings("serial")
public class ReplicationDestinationUnavailableException extends RuntimeException {
	public ReplicationDestinationUnavailableException() {}
	
	public ReplicationDestinationUnavailableException(String message, String replicationPath) {
		super(message);
	}
}
