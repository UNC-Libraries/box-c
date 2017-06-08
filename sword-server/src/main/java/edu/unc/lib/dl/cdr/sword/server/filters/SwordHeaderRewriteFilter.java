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
package edu.unc.lib.dl.cdr.sword.server.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Performs header rewrites to ensure backwards compatability between SWORD 1.x and 2.x.
 * @author bbpennel
 *
 */
public class SwordHeaderRewriteFilter extends OncePerRequestFilter implements ServletContextAware {
    private final static String PACKAGING = "Packaging";
    private final static String ON_BEHALF_OF = "On-Behalf-Of";

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String packaging = request.getHeader("X-Packaging");
        final String onBehalfOf = request.getHeader("X-On-Behalf-Of");

        if (packaging != null || onBehalfOf != null) {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if (PACKAGING.equals(name) && packaging != null) {
                        return packaging;
                    }
                    if (ON_BEHALF_OF.equals(name) && onBehalfOf != null) {
                        return onBehalfOf;
                    }
                    return super.getHeader(name);
                }
            };
            chain.doFilter(wrapper, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
