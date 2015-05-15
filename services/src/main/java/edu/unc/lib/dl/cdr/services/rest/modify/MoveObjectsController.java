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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
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
	
	private final Map<String, MoveRequest> activeMoveRequests;
	
	public MoveObjectsController() {
		activeMoveRequests = new HashMap<>();
	}

	@RequestMapping(value = "edit/move", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> moveObjects(@RequestBody MoveRequest moveRequest, HttpServletResponse response) {
		Map<String, Object> results = new HashMap<>();
		// Validate that the request contains the newPath and ids fields.
		if (moveRequest == null || moveRequest.ids == null || moveRequest.ids.size() == 0
				|| moveRequest.getNewParent() == null || moveRequest.getNewParent().length() == 0) {
			response.setStatus(400);
			results.put("error", "Request must provide a newParent destination and a list of ids");
			return results;
		}
		
		moveRequest.setUser(GroupsThreadStore.getUsername());

		Thread moveThread = new Thread(new MoveRunnable(moveRequest, GroupsThreadStore.getGroups()));
		log.info("User {} is starting move operation of {} objects to destination {}",
				new Object[] { GroupsThreadStore.getUsername(), moveRequest.ids.size(), moveRequest.getNewParent()});
		moveThread.start();

		response.setStatus(200);
		
		results.put("id", moveRequest.getMoveId());
		results.put("message", "Operation to move " + moveRequest.ids.size() + " objects into container "
				+ moveRequest.getNewParent() + " has begun");
		return results;
	}

	@RequestMapping(value = "listMoves", method = RequestMethod.GET)
	public @ResponseBody
	Object listMoves() {
		return this.activeMoveRequests.keySet();
	}

	@RequestMapping(value = "listMoves/{moveId}/objects", method = RequestMethod.GET)
	public @ResponseBody
	Object getMovedObjects(@PathVariable("moveId") String moveId) {
		MoveRequest request = this.activeMoveRequests.get(moveId);
		if (request == null) {
			return null;
		}
		
		return request.getIds();
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
		private String user;
		private String moveId;

		public MoveRequest() {
			moveId = UUID.randomUUID().toString();
		}

		public String getNewParent() {
			return newParent;
		}

		public void setNewParent(String newParent) {
			this.newParent = newParent;
		}

		public List<PID> getMoved() {
			List<PID> moved = new ArrayList<PID>(ids.size());
			for (String id : ids)
				moved.add(new PID(id));
			return moved;
		}

		public void setIds(List<String> ids) {
			this.ids = ids;
		}

		public List<String> getIds() {
			return ids;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getMoveId() {
			return moveId;
		}

		public void setMoveId(String moveId) {
			this.moveId = moveId;
		}
	}

	public class MoveRunnable implements Runnable {

		private final MoveRequest request;
		private final AccessGroupSet groups;

		public MoveRunnable(MoveRequest request, AccessGroupSet groups) {
			this.request = request;
			this.groups = groups;
		}

		@Override
		public void run() {
			try {
				activeMoveRequests.put(request.getMoveId(), request);
				
				GroupsThreadStore.storeGroups(groups);
				GroupsThreadStore.storeUsername(request.getUser());
				digitalObjectManager.move(request.getMoved(), new PID(request.getNewParent()),
						request.getUser(), "Moved through API");
				
				log.info("Finished move operation of {} objects to destination {} for user {}", new Object[] {
						request.getMoved().size(), request.getNewParent(), GroupsThreadStore.getUsername() });
			} catch (IngestException e) {
				log.error("Failed to move objects to {}", request.getNewParent(), e);
			} finally {
				activeMoveRequests.remove(request.getMoveId());
				GroupsThreadStore.clearStore();
			}
		}
	}
}
