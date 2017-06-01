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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * API controller for creating new containers
 *
 * @author bbpennel
 *
 */
@Controller
public class AddContainerController {
	private static final Logger log = LoggerFactory.getLogger(AddContainerController.class);

	@Autowired
	private DigitalObjectManager digitalObjectManager;
	
	@RequestMapping(value = "edit/create_container/{id}", method = RequestMethod.POST)
	public @ResponseBody
	String createContainer(@PathVariable("id") String parentId, @RequestParam("name") String name,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "description", required = false) MultipartFile description, Model model,
			HttpServletRequest request, HttpServletResponse response) {
		PID parent = new PID(parentId);
		try {
			String user = request.getHeader("On-Behalf-Of");
			if (user == null)
				user = request.getRemoteUser();
			
			ContentModelHelper.Model extraModel = null;
			if (type != null && type.equals("collection")) {
				extraModel = ContentModelHelper.Model.COLLECTION;
			} else if (type != null && type.equals("aggregate")) {
				extraModel = ContentModelHelper.Model.AGGREGATE_WORK;
			} 
			
			byte[] mods = null;
			if(description != null && !description.isEmpty()) {
				mods = description.getBytes();
			}

			PID containerPid = digitalObjectManager.createContainer(name, parent, extraModel, user, mods);
			response.setStatus(201);
			return "{\"pid\": \"" + containerPid.getPidAsString() + "\"}";
		} catch (IOException e) {
			log.error("Unexpected IO exception", e);
		} catch (IngestException e) {
			log.error("Unexpected exception", e);
		}
		response.setStatus(500);
		return "{\"error\": \"An error occurred while attempting to create a container in " + parent + "\"}";
	}

}
