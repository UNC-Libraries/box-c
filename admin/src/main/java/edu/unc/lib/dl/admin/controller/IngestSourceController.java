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
package edu.unc.lib.dl.admin.controller;

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

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.admin.collect.IngestSourceConfiguration;
import edu.unc.lib.dl.admin.collect.IngestSourceManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * @author bbpennel
 * @date Oct 23, 2015
 */
@Controller
public class IngestSourceController {
	private static final Logger log = LoggerFactory.getLogger(IngestSourceController.class);
	
	@Autowired
	private AccessControlService aclService;

	@Autowired
	private IngestSourceManager sourceManager;
	
	@Autowired
	private DepositStatusFactory depositStatusFactory;


	@RequestMapping(value = "listSourceCandidates/{pid}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody Object listCandidatesPackages(@PathVariable("pid") String pid, HttpServletResponse resp) {
		PID destination = new PID(pid);

		AccessGroupSet groups = GroupsThreadStore.getGroups();
		// Check that the user has permission to deposit to the destination
		if (!aclService.hasAccess(destination, groups, Permission.addRemoveContents)) {
			resp.setStatus(401);
			return null;
		}

		return sourceManager.listCandidates(destination);
	}

	@RequestMapping(value = "listSources/{pid}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody Object listIngestSources(@PathVariable("pid") String pid, HttpServletResponse resp) {
		PID destination = new PID(pid);

		AccessGroupSet groups = GroupsThreadStore.getGroups();
		// Check that the user has permission to deposit to the destination
		if (!aclService.hasAccess(destination, groups, Permission.addRemoveContents)) {
			resp.setStatus(401);
			return null;
		}

		return sourceManager.listSources(destination);
	}

	@RequestMapping(value = "ingestFromSource/{pid}", method = RequestMethod.POST, produces = "application/json")
	public void ingestFromSource(@PathVariable("pid") String pid,
			@RequestBody List<IngestPackageDetails> packages, HttpServletResponse resp) {
		
		log.error("Request to ingest from source to {}", pid);
		PID destPid = new PID(pid);

		// Check that user has permission to deposit to the selected destination
		if (!aclService.hasAccess(destPid, GroupsThreadStore.getGroups(), Permission.addRemoveContents)) {
			log.debug("Access denied to user {} while attempting to from ingest source deposit to {}",
					GroupsThreadStore.getUsername(), pid);
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		for (IngestPackageDetails packageDetails : packages) {
			// Verify that the package path is from within the allowed locations for the specified ingest source
			if (!sourceManager.isPathValid(packageDetails.getPackagePath(), packageDetails.getSourceId())) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		}
		
		// Build deposit entries and add to queue
		for (IngestPackageDetails packageDetails : packages) {
			IngestSourceConfiguration source = sourceManager.getSourceConfiguration(packageDetails.getSourceId());
			
			PID depositPID = new PID("uuid:" + UUID.randomUUID().toString());
			
			Map<String, String> deposit = new HashMap<String,String>();
			deposit.put(DepositField.sourcePath.name(), source.getBase() + packageDetails.getPackagePath());
			deposit.put(DepositField.fileName.name(), packageDetails.getLabel());
			deposit.put(DepositField.packagingType.name(), packageDetails.getPackagingType());
			deposit.put(DepositField.uuid.name(), depositPID.getUUID());
			deposit.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
			deposit.put(DepositField.depositorName.name(), GroupsThreadStore.getUsername());
			deposit.put(DepositField.permissionGroups.name(), GroupsThreadStore.getGroupString());
			deposit.put(DepositField.depositorEmail.name(), GroupsThreadStore.getUsername() + "@email.unc.edu");
			
			deposit.put(DepositField.containerId.name(), pid);
			deposit.put(DepositField.state.name(), DepositState.unregistered.name());
			deposit.put(DepositField.actionRequest.name(), DepositAction.register.name());
			
			this.depositStatusFactory.save(depositPID.getUUID(), deposit);
		}
		
		resp.setStatus(HttpServletResponse.SC_OK);
		return;
	}

	public static class IngestPackageDetails {
		private String sourceId;
		// Path is relative to the base for the source
		private String packagePath;
		private String label;
		private String packagingType;
		
		public IngestPackageDetails() {
		}

		public String getSourceId() {
			return sourceId;
		}

		public void setSourceId(String sourceId) {
			this.sourceId = sourceId;
		}

		public String getPackagePath() {
			return packagePath;
		}

		public void setPackagePath(String packagePath) {
			this.packagePath = packagePath;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getPackagingType() {
			return packagingType;
		}

		public void setPackagingType(String packagingType) {
			this.packagingType = packagingType;
		}
	}
}
