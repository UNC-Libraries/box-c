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

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.ResourceType;

/**
 * @author bbpennel
 * @date Jun 1, 2015
 */
@Controller
public class ContentModelController {
	
	private static final Logger log = LoggerFactory.getLogger(ContentModelController.class);
	
	@Autowired
	private DigitalObjectManager dom;

	@RequestMapping(value = "edit/changeType", method = RequestMethod.POST)
	public @ResponseBody Object changeResourceType(@RequestBody ChangeResourceTypeRequest changeRequest,
			HttpServletResponse response) {
		
		Map<String, Object> results = new HashMap<>();
		
		if (changeRequest.newType == null || changeRequest.newType.equals(ResourceType.File)) {
			results.put("error", "Invalid type " + changeRequest.newType
					+ " specified as the new type.  Only container types are supported currently.");
			response.setStatus(400);
			return results;
		}
		
		changeRequest.user = GroupsThreadStore.getUsername();
		changeRequest.groupSet = GroupsThreadStore.getGroups();
		
		ChangeTypeRunnable changeType = new ChangeTypeRunnable(changeRequest);
		Thread changeThread = new Thread(changeType);
		changeThread.start();
		
		results.put("message", "Operation to change " + changeRequest.pids.size() + " objects to type "
				+ changeRequest.newType + " has begun");
		
		response.setStatus(200);
		return results;
	}

	public static class ChangeResourceTypeRequest {
		private List<PID> pids;
		private ResourceType newTypeObject;
		private String newType;
		private String user;
		private AccessGroupSet groupSet;
		

		public ResourceType getNewType() {
			return newTypeObject;
		}

		public void setNewType(String newType) {
			this.newType = newType;
			this.newTypeObject = ResourceType.valueOf(newType);
		}

		public void setPids(List<String> pids) {
			this.pids = new ArrayList<PID>(pids.size());
			for (String id : pids)
				this.pids.add(new PID(id));
		}
		
		public List<PID> getPids() {
			return this.pids;
		}
	}
	
	private class ChangeTypeRunnable implements Runnable {
		
		private final ChangeResourceTypeRequest changeRequest;
		
		public ChangeTypeRunnable(ChangeResourceTypeRequest changeRequest) {
			this.changeRequest = changeRequest;
		}

		@Override
		public void run() {
			Long start = System.currentTimeMillis();
			
			try {
				GroupsThreadStore.storeGroups(changeRequest.groupSet);
				GroupsThreadStore.storeUsername(changeRequest.user);
				
				try {
					dom.changeResourceType(changeRequest.pids, changeRequest.getNewType(), changeRequest.user);
				} catch (UpdateException e) {
					log.warn("Failed to change model to {}", changeRequest.newType);
				}
			} finally {
				GroupsThreadStore.clearStore();
			}
			
			log.info("Finished changing content models for {} object(s) in {}ms",
					changeRequest.pids.size(), (System.currentTimeMillis() - start));
		}
		
	}
}
