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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.cdr.services.BatchIngestService;

/**
 * @author Gregory Jansen
 *
 */
@Controller
@RequestMapping(value={"/info", "/"})
public class InfoRestController implements ServletContextAware {
	private static final Logger LOG = LoggerFactory.getLogger(InfoRestController.class);

	private Map<String, Object> serviceInfo = new HashMap<String, Object>();
	private ServletContext servletContext = null;

	@Resource(name="contextUrl")
	protected String contextUrl = null;

	@Resource
	protected BatchIngestService batchIngestService = null;

	@PostConstruct
	public void init() {
		LOG.debug("init");
		serviceInfo = new HashMap<String, Object>();
		Map<String, Object> data = new HashMap<String, Object>();
		serviceInfo.put("serviceInfo", data);
		// add POM properties to serviceInfo
		Properties pomProperties = null;
		java.io.InputStream in = servletContext.getResourceAsStream(
				"META-INF/maven/edu.unc.lib.cdr/services/pom.properties");
		pomProperties = new Properties();
		try {
			pomProperties.load(in);
			data.put("version", pomProperties.get("version"));
			data.put("groupId", pomProperties.get("groupId"));
			data.put("artifactId", pomProperties.get("artifactId"));
		} catch (IOException e) {
			LOG.warn("REST service cannot load pom.properties", e);
		}
		// add urls to serviceInfo
		Map<String, Object> uris = new HashMap<String, Object>();
		data.put("uris", uris);
		uris.put("swordServiceUri", contextUrl+"/sword");
		uris.put("ingestServiceUri", contextUrl+"/rest/ingest");
		uris.put("enhancementServiceUri", contextUrl+"/rest/enhancement");
		uris.put("solrIndexServiceUri", contextUrl+"/rest/indexing");
		// { buildDate:"2011-10-26 11:50:56 UTC-0400",
		// currentUserUri:"https://example.org/path/to/user",
		// tasksUri:"https://example.org/path/to/tasks",
		// usersUri:"https://example.org/path/to/users",
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getInfo() {
		LOG.debug("getInfo()");
		return serviceInfo;
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

}
