package edu.unc.lib.boxc.web.common.auth.filters;

import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.getRemoteUser;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Http Filter which requires that the connection be made by an authenticated user
 *
 * @author bbpennel
 *
 */
public class RequireLoginFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequireLoginFilter.class);

    private String notLoggedInUrl;
    private boolean forwardRequest;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String user = getRemoteUser(request);
        if (isBlank(user)) {
            if (forwardRequest) {
                RequestDispatcher dispatcher = request.getRequestDispatcher(notLoggedInUrl);
                dispatcher.forward(request, response);
            } else {
                response.sendRedirect(notLoggedInUrl);
            }
            return;
        } else {
            log.debug("User logged in as {}", user);
            chain.doFilter(request, response);
        }
    }

    public void setNotLoggedInUrl(String notLoggedInUrl) {
        this.notLoggedInUrl = notLoggedInUrl;
    }

    public void setForwardRequest(boolean forwardRequest) {
        this.forwardRequest = forwardRequest;
    }
}