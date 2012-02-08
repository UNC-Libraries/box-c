/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.rest;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.cdr.services.processing.ServiceConductor;

/**
 * @author Gregory Jansen
 *
 */
public class AbstractServiceConductorRestController implements ServletContextAware {

	protected ServletContext servletContext = null;

	@Resource(name = "contextUrl")
	protected String contextUrl = null;

	/**
	 *
	 */
	public AbstractServiceConductorRestController() {
		super();
	}

	protected void addServiceConductorInfo(Map<String, Object> result, ServiceConductor c) {
		result.put("active", !c.isPaused());
		result.put("idle", c.isIdle());
		result.put("id", c.getIdentifier());
		result.put("active threads", c.getActiveThreadCount());
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

}