package edu.unc.lib.dl.httpclient;

import java.io.IOException;
import java.net.SocketException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry http request
 * @author count0
 *
 */
public class ConnectionInterruptedHttpMethodRetryHandler implements
        HttpRequestRetryHandler {
    private static final Logger log = LoggerFactory
            .getLogger(ConnectionInterruptedHttpMethodRetryHandler.class);

    private int retries = 5;
    private long retryDelay = 0;

    public ConnectionInterruptedHttpMethodRetryHandler(int retries,
            long retryDelay) {
        super();
        this.retries = retries;
        this.retryDelay = retryDelay;
    }

    @Override
    public boolean retryRequest(IOException e, int executionCount,
            HttpContext context) {
        if (executionCount >= retries) {
            return false;
        }

        if (e instanceof NoHttpResponseException
                || e instanceof SocketException) {
            log.warn("Connection interrupted, retrying connection");
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
