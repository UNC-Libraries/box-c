package edu.unc.lib.boxc.web.common.auth;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility for retrieving the remote user
 *
 * @author bbpennel
 *
 */
public class RemoteUserUtil {

    public static final String REMOTE_USER = "REMOTE_USER";

    private RemoteUserUtil() {
    }

    /**
     * Retrieve the remote user from the given request.
     *
     * @param request
     * @return remote user or an empty string if none found.
     */
    public static String getRemoteUser(HttpServletRequest request) {
        String user = request.getHeader(REMOTE_USER);
        if (user == null) {
            user = request.getRemoteUser();
        }
        if (user == null) {
            user = "";
        } else {
            user = user.trim();
        }
        return user;
    }
}
