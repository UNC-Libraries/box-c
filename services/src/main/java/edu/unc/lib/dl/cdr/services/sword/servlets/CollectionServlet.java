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
package edu.unc.lib.dl.cdr.services.sword.servlets;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.swordapp.server.CollectionAPI;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;

@Controller
@RequestMapping("/collection")
public class CollectionServlet extends BaseSwordServlet {
	private static Logger LOG = Logger.getLogger(CollectionServlet.class);

	@Resource
	protected CollectionListManager collectionListManager;
	@Resource
	protected CollectionDepositManager collectionDepositManager;
	protected CollectionAPI api;

	@PostConstruct
	public void init() throws ServletException {
		// load the API
		this.api = new CollectionAPI(this.collectionListManager, this.collectionDepositManager, this.config);
		//this.api = new CollectionAPIMultipart(this.collectionListManager, this.collectionDepositManager, this.config);
	}

	@RequestMapping(value = "/{pid}", method = RequestMethod.GET)
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		LOG.debug("GET request for collection content list");
		this.api.get(req, resp);
	}

	@RequestMapping(value = "/{pid}", method = RequestMethod.POST)
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		LOG.debug("POST request to submit to collection: " + req.getHeader("Content-Type"));
		this.api.post(req, resp);
	}

	public CollectionListManager getCollectionListManager() {
		return collectionListManager;
	}

	public void setCollectionListManager(CollectionListManager collectionListManager) {
		this.collectionListManager = collectionListManager;
	}

	public CollectionDepositManager getCollectionDepositManager() {
		return collectionDepositManager;
	}

	public void setCollectionDepositManager(CollectionDepositManager collectionDepositManager) {
		this.collectionDepositManager = collectionDepositManager;
	}
}
