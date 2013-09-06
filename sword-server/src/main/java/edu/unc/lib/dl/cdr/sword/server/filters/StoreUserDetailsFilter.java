package edu.unc.lib.dl.cdr.sword.server.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

/**
 * Stores details about the user which are not directly related to the SWORD protocol to the local thread
 * 
 * @author bbpennel
 * 
 */
public class StoreUserDetailsFilter extends OncePerRequestFilter implements ServletContextAware {
	private static final Logger log = LoggerFactory.getLogger(StoreUserDetailsFilter.class);

	protected static String EMAIL_HEADER = "mail";

	@Override
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		log.debug("Performing StoreUserDetailsFilter");
		// Capture the users email address if its available
		if (request.getHeader(EMAIL_HEADER) != null) {
			String emailAddress = request.getHeader(EMAIL_HEADER);
			if (emailAddress.endsWith("_UNC"))
				SwordConfigurationImpl.storeUserEmailAddress(emailAddress.substring(0, emailAddress.length() - 4));
		}
		try {
			chain.doFilter(request, response);
		} finally {
			// Clear out group store no matter what happens
			SwordConfigurationImpl.clearUserEmailAddress();
		}
	}
}
