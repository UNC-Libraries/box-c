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
package edu.unc.lib.dl.fedora;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * This class takes Shibboleth group memberships and stores them in the <code>GroupsThreadStore</code>
 * for the duration of the request.
 * @author count0
 *
 */
public class GroupsToThreadStoreInterceptor implements HandlerInterceptor {
	
	private static final Logger LOG = LoggerFactory.getLogger(GroupsToThreadStoreInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		LOG.debug("About to set cdr groups for request processing thread");
		String shibGroups = request.getHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER);
		if(shibGroups != null && shibGroups.trim().length() > 0) {
			GroupsThreadStore.storeGroups(shibGroups);
			LOG.debug("Setting cdr groups for request processing thread: "+shibGroups);
		} else {
			GroupsThreadStore.clearGroups();
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		LOG.debug("About to clear cdr groups for request processing thread");
		GroupsThreadStore.clearGroups();
		LOG.debug("Cleared cdr groups for request processing thread");
	}

}
