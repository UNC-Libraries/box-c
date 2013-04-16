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
package edu.unc.lib.dl.cdr.services.solr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Servlet which allows a subset of solr updating methods to be directly called via http request.
 * @author bbpennel
 *
 */
//@Controller
//@RequestMapping("/solrUpdate")
public class SolrUpdateServlet {

	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateServlet.class);

	@Autowired(required = true)
	private SolrUpdateService solrUpdateService;

	//@RequestMapping(method = RequestMethod.GET)
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pid = req.getParameter("pid");
		String action = req.getParameter("action");

		LOG.info("Solr Update Servlet request received: " + action + " on " + pid);

		if (action != null) {
			if (action.equals("reindex")) {
				if (pid != null) {
					solrUpdateService.offer(pid, IndexingActionType.CLEAN_REINDEX);
				}
			} else if (action.equals("reindexInplace")) {
				if (pid != null) {
					solrUpdateService.offer(pid, IndexingActionType.RECURSIVE_REINDEX);
				}
			} else if (action.equals("clearIndex")) {
				solrUpdateService.offer("", IndexingActionType.CLEAR_INDEX);
			}
		}
	}

	public SolrUpdateService getSolrUpdateService() {
		return solrUpdateService;
	}

	public void setSolrUpdateService(SolrUpdateService solrUpdateService) {
		this.solrUpdateService = solrUpdateService;
	}
}
