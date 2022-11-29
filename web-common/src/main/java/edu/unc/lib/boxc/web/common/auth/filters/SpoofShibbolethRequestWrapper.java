package edu.unc.lib.boxc.web.common.auth.filters;

import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.REMOTE_USER;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * RequestWrapper which pulls shibboleth authentication spoofing cookies from the
 * request and uses them to override both the remote user and groups.
 *
 * Only to be used for test instances.
 *
 * @author bbpennel
 *
 */
public class SpoofShibbolethRequestWrapper extends HttpServletRequestWrapper {

    private final static String SPOOF_COOKIE_PREFIX = "AUTHENTICATION_SPOOFING-";

    private String spoofEmailSuffix;
    private HashMap<String, String> values;

    private HttpServletRequest request;

    public SpoofShibbolethRequestWrapper(HttpServletRequest request, String spoofEmailSuffix) throws IOException {
        super(request);
        this.request = request;
        this.spoofEmailSuffix = spoofEmailSuffix;

        extractSpoofValues();
    }

    private void extractSpoofValues() throws IOException {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            values = new HashMap<>();

            for (Cookie c : cookies) {
                if (c.getName().startsWith(SPOOF_COOKIE_PREFIX)) {
                    String key = c.getName().substring(SPOOF_COOKIE_PREFIX.length());
                    String value = URLDecoder.decode(c.getValue(), "UTF-8");
                    values.put(key, value);
                }
            }
            if (request.getRemoteUser() == null && values.containsKey(REMOTE_USER)) {
                values.put("mail", values.get(REMOTE_USER) + spoofEmailSuffix);
            }
        }
    }

    @Override
    public String getRemoteUser() {
        if (values == null) {
            return super.getRemoteUser();
        }

        String remoteUser = request.getRemoteUser();
        if (remoteUser == null) {
            remoteUser = values.get(REMOTE_USER);
        }

        return remoteUser;
    }

    @Override
    public Principal getUserPrincipal() {
        if (values == null) {
            return super.getUserPrincipal();
        }

        String remoteUser = request.getRemoteUser();
        if (remoteUser == null) {
            remoteUser = values.get(REMOTE_USER);
        }

        final String user = remoteUser;

        return new Principal() {
            @Override
            public String getName() {
                return user;
            }
        };
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (values == null || !values.containsKey(name)) {
            return super.getHeaders(name);
        }

        return Collections.enumeration(Arrays.asList(values.get(name)));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        if (values == null) {
            return super.getHeaderNames();
        }

        // Combine existing header name enum with spoofed headers
        Set<String> headerSet = new HashSet<>();
        Enumeration<String> headerEnum = request.getHeaderNames();
        while (headerEnum.hasMoreElements()) {
            headerSet.add(headerEnum.nextElement());
        }
        headerSet.addAll(values.keySet());

        return Collections.enumeration(headerSet);
    }

    @Override
    public String getHeader(String name) {
        if (values == null || !values.containsKey(name)) {
            return super.getHeader(name);
        }

        return values.get(name);
    }
}
