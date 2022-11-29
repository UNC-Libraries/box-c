package edu.unc.lib.boxc.web.common.auth;

/**
 * Constants for headers related to HTTP authentication/authorization.
 *
 * @author count0
 */
public class HttpAuthHeaders {
    public static final String SHIBBOLETH_GROUPS_HEADER = "isMemberOf";
    public static final String FORWARDED_GROUPS_HEADER = "forwardedGroups";
    public static final String FORWARDED_MAIL_HEADER = "forwardedMail";

    private HttpAuthHeaders() {
    }
}
