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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Controller
public class MoveObjectsController {
	private static final Logger log = LoggerFactory.getLogger(MoveObjectsController.class);

	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;
	@Autowired
	private DigitalObjectManager digitalObjectManager;

	@RequestMapping(value = "edit/move", method = RequestMethod.POST)
	public @ResponseBody
	String moveObjects(@RequestBody MoveRequest moveRequest, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		// Validate that the request contains the newPath and ids fields.
		if (moveRequest == null || moveRequest.ids == null || moveRequest.ids.size() == 0
				|| moveRequest.getNewParent() == null || moveRequest.getNewParent().length() == 0) {
			response.setStatus(400);
			return "{\"error\": \"Request must provide a newParent destination and a list of ids\"}";
		}

		List<PID> pids = new ArrayList<PID>(moveRequest.getIds().size());
		for (String id : moveRequest.getIds())
			pids.add(new PID(id));
		PID parent = new PID(moveRequest.getNewParent());

		try {
			digitalObjectManager.move(pids, parent, GroupsThreadStore.getUsername(), "Moved through API");
		} catch (IngestException e) {
			log.error("Failed to move objects to " + parent, e);
			response.setStatus(500);
			return "{\"error\": \"An error occurred while attempting to move " + pids.size() + " objects into container "
					+ parent.getPid() + "\"}";
		}

		response.setStatus(204);
		return null;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public static class MoveRequest {
		private String newParent;
		private List<String> ids;

		public MoveRequest() {
		}

		public String getNewParent() {
			return newParent;
		}

		public void setNewParent(String newParent) {
			this.newParent = newParent;
		}

		public List<String> getIds() {
			return ids;
		}

		public void setIds(List<String> ids) {
			this.ids = ids;
		}
	}
}
