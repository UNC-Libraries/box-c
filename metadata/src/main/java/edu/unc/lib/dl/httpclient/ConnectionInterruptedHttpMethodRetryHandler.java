package edu.unc.lib.dl.httpclient;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionInterruptedHttpMethodRetryHandler implements HttpMethodRetryHandler {
	private static final Logger log = LoggerFactory.getLogger(ConnectionInterruptedHttpMethodRetryHandler.class);

	private int retries = 5;
	private long retryDelay = 0;
	
	public ConnectionInterruptedHttpMethodRetryHandler(int retries, long retryDelay) {
		super();
		this.retries = retries;
		this.retryDelay = retryDelay;
	}

	@Override
	public boolean retryMethod(final HttpMethod method, final IOException e, int executionCount) {
		if (executionCount >= retries) {
			return false;
		}
		if (e instanceof NoHttpResponseException || e instanceof SocketException) {
			log.warn("Connection interrupted, retrying connection to {}", method.getPath());
			if (retryDelay > 0) {
				try {
					Thread.sleep(retryDelay);
				} catch (InterruptedException e1) {
					log.warn("Interrupted while waiting to retry connect");
				}
			}
			return true;
		}
		return false;
	}

}
