package edu.unc.lib.cdr;

@SuppressWarnings("serial")
public class ReplicationException extends RuntimeException {
	public ReplicationException() {}
	
	public ReplicationException(String message, String binaryPath, String replicationPath) {
		super(message);
	}
}