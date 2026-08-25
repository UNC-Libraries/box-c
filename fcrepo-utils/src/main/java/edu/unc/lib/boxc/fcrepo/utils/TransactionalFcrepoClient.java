package edu.unc.lib.boxc.fcrepo.utils;

import java.net.URI;

import org.apache.http.client.methods.HttpRequestBase;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

/**
 * Wrapper class that retrieves a transaction uri from the location header in the response
 * @author harring, krwong
 *
 */
public class TransactionalFcrepoClient extends FcrepoClient {

    protected TransactionalFcrepoClient(String username, String password, String host,
                Boolean throwExceptionOnFailure) {
        super(username, password, host, throwExceptionOnFailure);
    }

    /**
     * Build a TransactionalFcrepoClient
     *
     * @return a client builder
     */
    public static TransactionalFcrepoClientBuilder client() {
        return new TransactionalFcrepoClientBuilder();
    }

    /**
     * Execute a HTTP request and include the transaction uri in the Atomic-ID request header
     *
     * @param uri URI the request is made to
     * @param request the request
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    @Override
    public FcrepoResponse executeRequest(URI uri, HttpRequestBase request)
            throws FcrepoOperationFailedException {
        if (hasTxId() && !uri.toString().contains("/fcr:tx")) {
            URI txUri = FedoraTransaction.txUriThread.get();
            request.setHeader("Atomic-ID", txUri.toString());
        }

        return super.executeRequest(uri, request);
    }

    private boolean hasTxId() {
        return FedoraTransaction.hasTxId();
    }

    public static class TransactionalFcrepoClientBuilder extends FcrepoClientBuilder {

        private String authUser;

        private String authPassword;

        private String authHost;

        private boolean throwExceptionOnFailure;

        /**
         * Add basic authentication credentials to this client
         *
         * @param username username for authentication
         * @param password password for authentication
         * @return the client builder
         */
        @Override
        public TransactionalFcrepoClientBuilder credentials(final String username, final String password) {
            this.authUser = username;
            this.authPassword = password;
            return this;
        }

        /**
         * Add an authentication scope to this client
         *
         * @param authHost authentication scope value
         * @return this builder
         */
        @Override
        public TransactionalFcrepoClientBuilder authScope(final String authHost) {
            this.authHost = authHost;
            return this;
        }

        /**
         * Client should throw exceptions when failures occur
         *
         * @return this builder
         */
        @Override
        public TransactionalFcrepoClientBuilder throwExceptionOnFailure() {
            this.throwExceptionOnFailure = true;
            return this;
        }

        /**
         * Get the client
         *
         * @return the client constructed by this builder
         */
        @Override
        public TransactionalFcrepoClient build() {
            return new TransactionalFcrepoClient(authUser, authPassword, authHost, throwExceptionOnFailure);
        }
    }
}
