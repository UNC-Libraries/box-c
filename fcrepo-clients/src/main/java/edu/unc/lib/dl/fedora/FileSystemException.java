package edu.unc.lib.dl.fedora;

import org.springframework.ws.soap.client.SoapFaultClientException;

public class FileSystemException extends FedoraException {
	private static final long serialVersionUID = -4711418067046583942L;

	public FileSystemException(SoapFaultClientException e) {
		super(e);
	}

	public FileSystemException(String message) {
		super(message);
	}
}
