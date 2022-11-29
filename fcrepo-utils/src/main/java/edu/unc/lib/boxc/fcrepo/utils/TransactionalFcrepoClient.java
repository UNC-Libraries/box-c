package edu.unc.lib.boxc.fcrepo.utils;

import static org.fcrepo.client.FedoraTypes.LDP_NON_RDF_SOURCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoLink;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * This wrapper class rewrites uris to have a transaction id (txid) in them, if one is present on the current thread
 * If no txid is present, class behavior is identical to FcrepoClient
 * @author harring
 *
 */
public class TransactionalFcrepoClient extends FcrepoClient {

    private static final String REST = "/rest";
    private static final List<String> RDF_MIMETYPES = Arrays.asList(new String[] {
            "application/sparql-update", "text/turtle", "text/rdf+n3", "application/n3",
            "text/n3", "application/rdf+xml", "application/n-triples", "application/ld+json"});

    private static final String TX_ID_REGEX = "(tx:[a-z0-9\\-]+/)?";
    private static final String TX_RESPONSE_REGEX = "(tx:[a-z0-9\\-]+/)";

    private Pattern txBasePattern;
    private Pattern txRemovePattern;

    protected TransactionalFcrepoClient(String username, String password, String host,
                Boolean throwExceptionOnFailure, String baseUri) {
        super(username, password, host, throwExceptionOnFailure);
        String base = baseUri + (baseUri.endsWith("/") ? "" : "/");
        txBasePattern = Pattern.compile(base + TX_ID_REGEX);
        txRemovePattern = Pattern.compile(TX_RESPONSE_REGEX);
    }

    /**
     * Build a TransactionalFcrepoClient
     *
     * @return a client builder
     */
    public static TransactionalFcrepoClientBuilder client(String baseUri) {
        return new TransactionalFcrepoClientBuilder(baseUri);
    }

    /**
     * Execute a HTTP request, and modify the request to handle a transaction, if one is currently open
     *
     * @param url URI the request is made to
     * @param request the request
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    @Override
    public FcrepoResponse executeRequest(URI uri, HttpRequestBase request)
            throws FcrepoOperationFailedException {
        URI requestUri = uri;

        if (hasTxId()) {
            // Rewrite fedora resource URIs within RDF request body to include tx id
            if (needsRequestBodyRewrite(request)) {
                rewriteRequestBodyUris(request);
            }
            if (!uri.toString().contains("tx:")) {
                requestUri = rewriteUri(requestUri);
                request.setURI(requestUri);
            }
            FcrepoResponse resp = super.executeRequest(requestUri, request);
            if (!request.getMethod().equals("HEAD")) {
                // Strip tx ids out of response so they are invisible to clients
                return rewriteResponseBodyUris(resp);
            }
        }
        return super.executeRequest(requestUri, request);
    }

    /**
     * Removes tx ids from the response body
     */
    private FcrepoResponse rewriteResponseBodyUris(FcrepoResponse resp) {
        // Check that the response is RDF
        String contentType = resp.getContentType();
        if (contentType == null) {
            return resp;
        }
        // Trim off the encoding portion of the content type if present
        int index = contentType.indexOf(';');
        if (index != -1) {
            contentType = contentType.substring(0, index);
        }
        if (!RDF_MIMETYPES.contains(contentType)) {
            return resp;
        }

        try {
            String bodyString = IOUtils.toString(resp.getBody(), StandardCharsets.UTF_8);
            Matcher m = txRemovePattern.matcher(bodyString);
            String replacementBody = m.replaceAll("");
            resp.setBody(new ByteArrayInputStream(replacementBody.getBytes()));
            return resp;
        } catch (IOException e) {
            throw new FedoraException("Failed to read response from Fedora", e);
        }
    }

    /**
     * Rewrites a resource uri to include a tx id
     */
    private URI rewriteUri(URI rescUri) {
        // Transaction uri may be based on host header, rebase to actual fedora base uri
        URI txUri = FedoraTransaction.txUriThread.get();
        String rescId = rescUri.toString();
        // locate the rest component of the path, everything after is the
        // relative path to the resource
        int txIdIndex = rescId.indexOf(REST);
        // Get index where the relative path would begin
        int rescPathIndex = txIdIndex + REST.length();
        if (rescPathIndex == rescId.length()) {
            // If there is no relative path (is root of the repository), return the transaction uri
            return txUri;
        }
        // Insert transaction id before the relative path, construct rewritten uri
        rescId = rescId.substring(rescPathIndex);
        return URI.create(URIUtil.join(txUri, rescId));
    }

    private boolean hasTxId() {
        return FedoraTransaction.hasTxId();
    }

    /**
     * Checks the request to see whether it is a PUT, POST, or PATCH and has an RDF mimetype
     */
    private boolean needsRequestBodyRewrite(HttpRequestBase request) {
        org.apache.http.Header contentTypeHeader = request.getFirstHeader("Content-Type");
        if (contentTypeHeader == null) {
            return false;
        }
        // Skip rewrite of binary objects/non-rdf sources
        Header[] links = request.getHeaders("Link");
        if (links != null && links.length > 0) {
            for (Header link: links) {
                FcrepoLink fcrepoLink = FcrepoLink.valueOf(link.getValue());
                if (fcrepoLink.getRel().equals("type")
                        && fcrepoLink.getUri().toString().equals(LDP_NON_RDF_SOURCE)) {
                    return false;
                }
            }
        }
        String contentType = contentTypeHeader.getValue();
        // request method is POST, PUT, or PATCH AND has one of the whitelisted mimetypes
        return request.getMethod().startsWith("P") && RDF_MIMETYPES.contains(contentType);
    }
     /**
      * Replaces all uris in the request body with tx uris
      */
    private HttpRequestBase rewriteRequestBodyUris(HttpRequestBase request) {
        HttpEntity requestBody = ((HttpEntityEnclosingRequestBase) request).getEntity();
        if (requestBody != null) {
            String bodyString = null;
            try (InputStream stream = requestBody.getContent()) {
                bodyString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new FedoraException("Could not stream content from body of request", e);
            }

            String fullTxPath = FedoraTransaction.txUriThread.get().toString();
            if (fullTxPath.lastIndexOf('/') != fullTxPath.length() - 1) {
                fullTxPath += "/";
            }

            Matcher m = txBasePattern.matcher(bodyString);
            String replacementBody = m.replaceAll(fullTxPath);
            InputStream replacementStream = new ByteArrayInputStream(replacementBody.getBytes());
            InputStreamEntity replacementEntity = new InputStreamEntity(replacementStream);
            ((HttpEntityEnclosingRequestBase) request).setEntity(replacementEntity);
            return request;
        }
        return null;
    }

    public static class TransactionalFcrepoClientBuilder extends FcrepoClientBuilder {

        private String authUser;

        private String authPassword;

        private String authHost;

        private boolean throwExceptionOnFailure;

        private String baseUri;

        public TransactionalFcrepoClientBuilder(String baseUri) {
            this.baseUri = baseUri;
        }

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
         * Add a fedora base uri to this client
         *
         * @param baseUri the base uri including "/rest/"
         * @return this builder
         */
        public TransactionalFcrepoClientBuilder baseUri(final String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        /**
         * Get the client
         *
         * @return the client constructed by this builder
         */
        @Override
        public TransactionalFcrepoClient build() {
            return new TransactionalFcrepoClient(authUser, authPassword, authHost,
                    throwExceptionOnFailure, baseUri);
        }
    }
}
