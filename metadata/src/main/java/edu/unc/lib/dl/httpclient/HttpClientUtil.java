/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.httpclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

/**
 * Utitlity class for common <code>HttpClient</code> operations.
 * 
 * @author count0
 */
public class HttpClientUtil {
    public static final String SHIBBOLETH_GROUPS_HEADER = "isMemberOf";
    public static final String FORWARDED_GROUPS_HEADER = "forwardedGroups";
    public static final String FORWARDED_MAIL_HEADER = "forwardedMail";

    private HttpClientUtil() {
    }

    /**
     * Gets a client that is configured to use HTTP Basic authentication.
     * 
     * @param urlString
     *            the URL that will eventually be fetched.
     * @param user
     *            the username to use.
     * @param pass
     *            the password to use
     * @return a client object configured to supply credentials to the URL
     *         specified by <code>urlString</code>.
     * @throws IllegalArgumentException
     *             if <code>urlString</code> is not a valid URL
     */
    public static CloseableHttpClient getAuthenticatedClient(String urlString,
            String user, String pass) {
        return getAuthenticatedClient(urlString, user, pass, null);
    }

    public static CloseableHttpClient getAuthenticatedClient(String urlString,
            String user, String pass,
            HttpClientConnectionManager connectionManager) {

        HttpClientBuilder builder = getAuthenticatedClientBuilder(urlString,
                user, pass);
        if (connectionManager != null) {
            builder.setConnectionManager(connectionManager);
        }

        return builder.build();
    }

    public static HttpClientBuilder getAuthenticatedClientBuilder(String host,
            String user, String pass) {
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope scope = null;

        if (host == null || host.length() == 0) {
            scope = new AuthScope(AuthScope.ANY);
        } else {
            scope = new AuthScope(new HttpHost(host));
        }

        credsProvider.setCredentials(scope, new UsernamePasswordCredentials(
                user, pass));

        HttpClientBuilder builder = HttpClients.custom().useSystemProperties()
                .setDefaultCredentialsProvider(credsProvider)
                .addInterceptorFirst(new PreemptiveAuthInterceptor());
        return builder;
    }

    static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context)
                throws HttpException, IOException {
            final AuthState authState = (AuthState) context
                    .getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            // If no auth scheme available yet, try to initialize it
            // preemptively
            if (authState.getAuthScheme() == null) {
                final CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(HttpClientContext.CREDS_PROVIDER);
                final HttpHost targetHost = (HttpHost) context
                        .getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                final AuthScope authScope = new AuthScope(
                        targetHost.getHostName(), targetHost.getPort());
                final Credentials creds = credsProvider
                        .getCredentials(authScope);
                if (creds == null) {
                    throw new HttpException(
                            "No credentials for preemptive authentication");
                }
                authState.update(new BasicScheme(), creds);
            }
        }
    }

    public static CredentialsProvider getCredentialsProvider(String queryURL,
            String user, String pass) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(getAuthenticationScope(queryURL),
                new UsernamePasswordCredentials(user, pass));
        return credsProvider;
    }

    /**
     * Generates a limited authentication scope for the supplied URL, so that an
     * HTTP client will not send username and passwords to other URLs.
     * 
     * @param queryURL
     *            the URL for the query.
     * @return an authentication scope tuned to the requested URL.
     * @throws IllegalArgumentException
     *             if <code>queryURL</code> is not a well-formed URL.
     */
    public static AuthScope getAuthenticationScope(String queryURL) {
        if (queryURL == null) {
            throw new NullPointerException(
                    "Cannot derive authentication scope for null URL");
        }
        try {
            URL url = new URL(queryURL);
            // port defaults to 80 unless the scheme is https
            // or the port is explicitly set in the URL.
            int port = 80;
            if (url.getPort() == -1) {
                if ("https".equals(url.getProtocol())) {
                    port = 443;
                }
            } else {
                port = url.getPort();
            }
            return new AuthScope(url.getHost(), port);
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("supplied URL <" + queryURL
                    + "> is ill-formed:" + mue.getMessage());
        }
    }
}
