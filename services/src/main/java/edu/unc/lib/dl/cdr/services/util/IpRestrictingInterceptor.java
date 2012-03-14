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
package edu.unc.lib.dl.cdr.services.util;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class IpRestrictingInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(IpRestrictingInterceptor.class);
	private Pattern allowRegEx = null;

	public String getAllowRegEx() {
		if (allowRegEx == null) {
			return null;
		}
		return allowRegEx.toString();
	}

	public void setAllowRegEx(String allowRegEx) {
		if (allowRegEx == null || allowRegEx.length() == 0) {
			this.allowRegEx = null;
		} else {
			this.allowRegEx = Pattern.compile(allowRegEx);
		}
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (allowRegEx != null && allowRegEx.matcher(request.getRemoteAddr()).matches()) {
			return super.preHandle(request, response, handler);
		} else {
			LOG.warn("Access denied to " + request.getRequestURL().toString() + " from " + request.getRemoteAddr());
			response.sendError(403);
			return false;
		}
	}

}
