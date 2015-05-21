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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	
	private final Long TIME_TO_LIVE_AFTER_FINISH = 12000L;
	
	private final Map<String, MoveRequest> activeMoveRequests;
	
	public MoveObjectsController() {
		activeMoveRequests = new HashMap<>();
	}

	@RequestMapping(value = "edit/move", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> moveObjects(@RequestBody MoveRequest moveRequest, HttpServletResponse response) {
		Map<String, Object> results = new HashMap<>();
		// Validate that the request contains the newPath and ids fields.
		if (moveRequest == null || moveRequest.moved == null || moveRequest.moved.size() == 0
				|| moveRequest.getDestination() == null || moveRequest.getDestination().length() == 0) {
			response.setStatus(400);
			results.put("error", "Request must provide a destination destination and a list of ids");
			return results;
		}
		
		moveRequest.setUser(GroupsThreadStore.getUsername());

		Thread moveThread = new Thread(new MoveRunnable(moveRequest, GroupsThreadStore.getGroups()));
		log.info("User {} is starting move operation of {} objects to destination {}",
				new Object[] { GroupsThreadStore.getUsername(), moveRequest.moved.size(), moveRequest.getDestination()});
		moveThread.start();

		response.setStatus(200);
		
		results.put("id", moveRequest.id);
		results.put("message", "Operation to move " + moveRequest.moved.size() + " objects into container "
				+ moveRequest.getDestination() + " has begun");
		return results;
	}

	@RequestMapping(value = "listMoves", method = RequestMethod.GET)
	public @ResponseBody
	Object listMoves() {
		Map<String, Boolean> moves = new HashMap<>(this.activeMoveRequests.size());
		
		Iterator<Entry<String, MoveRequest>> moveIt = activeMoveRequests.entrySet().iterator();
		while (moveIt.hasNext()) {
			Entry<String, MoveRequest> move = moveIt.next();
			long finishedAt = move.getValue().finishedAt;
			if (finishedAt == -1 || finishedAt + TIME_TO_LIVE_AFTER_FINISH > System.currentTimeMillis()) {
				moves.put(move.getKey(), finishedAt == -1);
			} else {
				moveIt.remove();
			}
		}
		
		return moves;
	}

	@RequestMapping(value = "listMoves/{moveId}/objects", method = RequestMethod.GET)
	public @ResponseBody
	Object getMovedObjects(@PathVariable("moveId") String moveId) {
		MoveRequest request = this.activeMoveRequests.get(moveId);
		if (request == null) {
			return null;
		}
		
		return request.getMoved();
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public static class MoveRequest {
		private String destination;
		private List<String> moved;
		private String user;
		private final String id;
		private long finishedAt = -1;

		public MoveRequest() {
			id = UUID.randomUUID().toString();
		}

		public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public List<PID> getMovedPids() {
			List<PID> movedPids = new ArrayList<PID>(moved.size());
			for (String id : moved)
				movedPids.add(new PID(id));
			return movedPids;
		}

		public void setMoved(List<String> moved) {
			this.moved = moved;
		}

		public List<String> getMoved() {
			return moved;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getId() {
			return id;
		}

		public long getFinishedAt() {
			return finishedAt;
		}

		public void setFinishedAt(long finishedAt) {
			this.finishedAt = finishedAt;
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
				activeMoveRequests.put(request.getId(), request);
				
				GroupsThreadStore.storeGroups(groups);
				GroupsThreadStore.storeUsername(request.getUser());
				digitalObjectManager.move(request.getMovedPids(), new PID(request.getDestination()),
						request.getUser(), "Moved through API");
				
				log.info("Finished move operation of {} objects to destination {} for user {}", new Object[] {
						request.getMoved().size(), request.getDestination(), GroupsThreadStore.getUsername() });
			} catch (IngestException e) {
				log.error("Failed to move objects to {}", request.getDestination(), e);
			} finally {
				request.setFinishedAt(System.currentTimeMillis());
				GroupsThreadStore.clearStore();
			}
		}
	}
}
