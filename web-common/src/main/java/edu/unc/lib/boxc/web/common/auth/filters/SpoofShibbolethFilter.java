package edu.unc.lib.boxc.web.common.auth.filters;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter which enables shibboleth header spoofing for testing purposes
 *
 * @author bbpennel
 *
 */
public class SpoofShibbolethFilter extends OncePerRequestFilter implements ServletContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpoofShibbolethFilter.class);

    private boolean spoofEnabled = false;
    private String spoofEmailSuffix = "@localhost";

    @PostConstruct
    public void init() {
        if (spoofEnabled) {
            log.warn("****Warning: Application started with spoofing filter enabled****");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (spoofEnabled) {
            filterChain.doFilter(new SpoofShibbolethRequestWrapper(request, spoofEmailSuffix), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    public boolean isSpoofEnabled() {
        return spoofEnabled;
    }

    public void setSpoofEnabled(boolean spoofEnabled) {
        this.spoofEnabled = spoofEnabled;
    }

    public void setSpoofEmailSuffix(String spoofEmailSuffix) {
        this.spoofEmailSuffix = spoofEmailSuffix;
    }
}
