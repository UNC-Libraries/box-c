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
package edu.unc.lib.dl.acl.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;

/**
 * @author Gregory Jansen
 * 
 */
public class SpoofShibbolethHeadersValve extends ValveBase {

    private final static String SPOOF_COOKIE_PREFIX = "AUTHENTICATION_SPOOFING-";

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.catalina.valves.ValveBase#invoke(org.apache.catalina.connector
     * .Request, org.apache.catalina.connector.Response)
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {

            // Pull spoofed values (those beginning with the prefix defined above) in from cookies

            HashMap<String, String> values = new HashMap<String, String>();

            for (Cookie c : cookies) {
                if (c.getName().startsWith(SPOOF_COOKIE_PREFIX)) {
                    String key = c.getName().substring(SPOOF_COOKIE_PREFIX.length());
                    String value = URLDecoder.decode(c.getValue(), "UTF-8");
                    values.put(key, value);
                }
            }

            // Set spoofed values on the request headers

            MimeHeaders headers = request.getCoyoteRequest().getMimeHeaders();

            for (Entry<String, String> ent : values.entrySet()) {
                headers.removeHeader(ent.getKey());
                MessageBytes memb = headers.addValue(ent.getKey());
                memb.setString(ent.getValue());
            }

            // Use the REMOTE_USER value to set a spoofed principal

            if (values.containsKey("REMOTE_USER")) {
                String remoteUser = values.get("REMOTE_USER");
                final String credentials = "credentials";
                final List<String> roles = new ArrayList<String>();
                final Principal principal = new GenericPrincipal(remoteUser, credentials, roles);
                request.setUserPrincipal(principal);
            }

            // Use the REMOTE_USER value to set a spoofed mail header

            if (values.containsKey("REMOTE_USER")) {
                String remoteUser = values.get("REMOTE_USER");
                headers.removeHeader("mail");
                MessageBytes memb = headers.addValue("mail");
                memb.setString(remoteUser + "@fake.spoof");
            }

        }

        getNext().invoke(request, response);

    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        System.err.println("WARNING WARNING " + this.getClass().getName() + " is configured for "
                + this.getContainer().getName());
    }
}
