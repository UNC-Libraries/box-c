package edu.unc.lib.dl.fedora;

import org.springframework.ws.soap.client.SoapFaultClientException;

public class AuthorizationException extends FedoraException {
	private static final long serialVersionUID = 2177327948413175683L;

	public AuthorizationException(String message) {
		super(message);
	}
	
	public AuthorizationException(SoapFaultClientException e) {
		super(e);
	}
}
