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
package edu.unc.lib.dl.security.access;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 * 
 */
public class SpoofShibbolethHeadersFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(SpoofShibbolethHeadersFilter.class);

	String remoteUser = null;
	String isMemberOfHeader = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig filterConfig) throws ServletException {
		remoteUser = filterConfig.getInitParameter("remoteUser");
		isMemberOfHeader = filterConfig.getInitParameter("isMemberOfHeader");
		LOG.warn("SpoofShibbolethHeadersFilter IS CONFIGURED TO SPOOF SHIBBOLETH HEADERS!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
	 * javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest) {
			String members = (String) ((HttpServletRequest) request).getHeader("isMemberOf");

			LOG.debug("Unspoofed isMemberOf: " + members);
			if (!(((HttpServletRequest) request).getServletPath().startsWith("/js/")
					|| ((HttpServletRequest) request).getServletPath().startsWith("/css/") || ((HttpServletRequest) request)
					.getServletPath().startsWith("/images/"))) {

				request = new javax.servlet.http.HttpServletRequestWrapper((HttpServletRequest) request) {

					@Override
					public String getHeader(String name) {
						if ("isMemberOf".equals(name)) {
							return isMemberOfHeader;
						} else {
							return super.getHeader(name);
						}
					}

					@Override
					public String getRemoteUser() {
						return remoteUser;
					}

				};
				LOG.warn("SpoofShibbolethHeadersFilter: remoteUser = " + remoteUser);
				LOG.warn("SpoofShibbolethHeadersFilter: isMemberOf header = " + isMemberOfHeader);
			}
			chain.doFilter(request, response);
		} else {
			throw new RuntimeException("Not an HTTP request.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
