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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Utitlity class for common <code>HttpClient</code> operations.
 */
public class HttpClientUtil {
	public static final String SHIBBOLETH_GROUPS_HEADER = "isMemberOf";
	public static final String FORWARDED_GROUPS_HEADER = "forwardedGroups";

    /**
     * Gets a client that is configured to use HTTP Basic authentication.
     * @param urlString the URL that will eventually be fetched.
     * @param user the username to use.
     * @param pass the password to use
     * @return a client object configured to supply credentials to the
     * URL specified by <code>urlString</code>.
     * @throws IllegalArgumentException if <code>urlString</code> is not a valid URL
     */
    public static HttpClient getAuthenticatedClient(String urlString, String user, String pass) {
        if ( urlString == null ) {
            throw new IllegalArgumentException("Cannot create HttpClient for null URL");
        }
        HttpClient client = new HttpClient();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user,pass);
        client.getState().setCredentials(getAuthenticationScope(urlString), creds);
        return client;
    }


    /**
     * Generates a limited authentication scope for the supplied URL, so that an HTTP client
     * will not send username and passwords to other URLs. 
     * @param queryURL the URL for the query.
     * @return an authentication scope tuned to the requested URL.
     * @throws IllegalArgumentException if <code>queryURL</code> is not a well-formed URL.
     */
    public static AuthScope getAuthenticationScope(String queryURL) {
        if ( queryURL == null ) {
            throw new NullPointerException("Cannot derive authentication scope for null URL");
        }
        try {
            URL url = new URL(queryURL);
            // port defaults to 80 unless the scheme is https
            // or the port is explicitly set in the URL.
            int port = 80;
            if ( url.getPort() == - 1 ) {
                if ( "https".equals(url.getProtocol()) ) {
                    port = 443;
                }
            } else {
                port = url.getPort();
            }
            return new AuthScope(url.getHost(), port);
        } catch(MalformedURLException mue) {
            throw new IllegalArgumentException("supplied URL <" + queryURL + "> is ill-formed:" + mue.getMessage());
        }
    }
}
