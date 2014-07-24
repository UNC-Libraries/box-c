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

import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.AGGREGATE_WORK;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

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
	private File depositsDirectory;
	@Autowired
	private DepositStatusFactory depositStatusFactory;

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

			// Store the mods in a byte array
			InputStream mods = null;
			if (description != null && !description.isEmpty()) {
				mods = description.getInputStream();
			}

			PID depositPID = makeDeposit(mods);
			registerDeposit(depositPID, parentId, name, type, user, mods);

			response.setStatus(201);
			return "{\"deposit\": \"" + depositPID.getPid() + "\"}";
		} catch (IOException e) {
			log.error("Unexpected IO exception", e);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		}
		response.setStatus(500);
		return "{\"error\": \"An error occurred while attempting to create a container in " + parent + "\"}";
	}

	private PID makeDeposit(InputStream mods) {

		UUID depositUUID = UUID.randomUUID();
		PID depositPID = new PID("uuid:" + depositUUID.toString());

		if (mods != null) {

			File depositDir = new File(depositsDirectory, depositPID.getUUID());
			depositDir.mkdir();

			File dataDir = new File(depositDir, "data");
			dataDir.mkdir();

			File modsFile = new File(dataDir, "mods.xml");
			OutputStream outStream = null;
			try {
				outStream = new FileOutputStream(modsFile);
				IOUtils.copy(mods, outStream);
			} catch (IOException e) {
				log.error("Failed to write MODS description to file while creating container", e);
			} finally {
				IOUtils.closeQuietly(outStream);
				IOUtils.closeQuietly(mods);
			}
		}

		return depositPID;
	}

	private void registerDeposit(PID depositPID, String parentId, String name, String type, String user,
			InputStream mods) {

		Map<String, String> status = new HashMap<String, String>();
		status.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.getUri());

		status.put(DepositField.uuid.name(), depositPID.getUUID());
		status.put(DepositField.depositSlug.name(), name);
		status.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(DepositField.depositorName.name(), user);
		status.put(DepositField.containerId.name(), parentId);
		status.put(DepositField.depositMethod.name(), DepositMethod.CDRAPI1.getLabel());
		// Skip deposit record for this tiny ingest
		status.put(DepositField.excludeDepositRecord.name(), "true");

		if (type != null && type.equals("collection")) {
			status.put(hasModel.toString(), COLLECTION.toString());
		} else if (type != null && type.equals("aggregate")) {
			status.put(hasModel.toString(), AGGREGATE_WORK.toString());
		} else {
			status.put(hasModel.toString(), CONTAINER.toString());
		}

		status.put(DepositField.permissionGroups.name(), GroupsThreadStore.getGroupString());

		status.put(DepositField.state.name(), DepositState.unregistered.name());
		status.put(DepositField.actionRequest.name(), DepositAction.register.name());

		Set<String> nulls = new HashSet<String>();
		for (String key : status.keySet()) {
			if (status.get(key) == null)
				nulls.add(key);
		}
		for (String key : nulls)
			status.remove(key);
		this.depositStatusFactory.save(depositPID.getUUID(), status);
	}
}
