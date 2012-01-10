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
/**
 * 
 */
package edu.unc.lib.dl.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.support.RequestContext;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.CreateCollectionObject;
import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.services.FolderManager;
import edu.unc.lib.dl.ui.util.UiUtilityMethods;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.MediatedSubmitDAO;

/**
 * 
 * 
 */
public class CreateCollectionController extends SimpleFormController {

	private UiWebService uiWebService;
	private FolderManager folderManager;
	private AgentFactory agentManager;
	private UiUtilityMethods uiUtilityMethods;


	
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {


		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		boolean noErrors = true;


		
		RequestContext requestContext = new RequestContext(request);

		MediatedSubmitDAO dao = (MediatedSubmitDAO) command;

		dao.setMessage(null);

		MultipartFile metadata = dao.getMetadata();

		logger.debug("dao filepath: '"+dao.getFilePath()+"'");
		
		CreateCollectionObject ingest = new CreateCollectionObject();

			if ((metadata != null) && (metadata.getSize() > 0)) {
				ingest.setMetadata(metadata.getBytes());

				if (uiUtilityMethods.notNull(dao.getFilePath())) {

					PID ownerPid = new PID(dao.getOwnerPid());
					try {
						Agent mediator = agentManager.findPersonByOnyen(request.getRemoteUser(), false);

						Agent ownerAgent = agentManager.getAgent(ownerPid,
								false);
					} catch (Exception e) {
						e.printStackTrace();
					}

					ingest.setFilePath(dao.getFilePath());

					if (uiUtilityMethods.notNull(dao.getOwnerPid())) {
						ingest.setOwnerPid(dao.getOwnerPid());
					} else {
						dao.setMessage(requestContext
								.getMessage("submit.owner.missing"));
						noErrors = false;
					}
				} else {
					dao.setMessage(requestContext
							.getMessage("submit.repository.path.missing"));
					noErrors = false;
				}
			} else {
				dao.setMessage(requestContext
						.getMessage("submit.metadata.missing"));
				noErrors = false;
			}

		logger.debug("noErrors is: " + noErrors);
		logger.debug(dao.getMessage());

		if (noErrors) {
			ingest.setAdminOnyen(request.getRemoteUser());
			ingest.setSubmissionCheck(dao.isSubmissionCheck());
			
			CreateCollectionObject wsResponse = new CreateCollectionObject();

			wsResponse = uiWebService.createCollection(ingest);

			if ((wsResponse != null)
					&& (Constants.SUCCESS.equals(wsResponse.getMessage()))) {

				String message = requestContext.getMessage("submit.collection.created",
						"");
				message += dao.getFilePath();

				dao.setMessage(message);

				return new ModelAndView("createcollection", "mediatedSubmitDAO", dao);

			} else { // something went wrong upstream
				dao
						.setMessage(requestContext
								.getMessage("submit.collection.error"));

				logger.debug("Create collection failure");
				if (wsResponse != null) {
					logger.debug(wsResponse.getMessage());
				}
			}
		}

		Map model = errors.getModel();
		model.put("mediatedSubmitDAO", dao);

		return new ModelAndView("createcollection", model);
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		MediatedSubmitDAO object = new MediatedSubmitDAO();

		UserGroupDAO userGroupRequest = new UserGroupDAO();
		userGroupRequest.setType(Constants.GET_GROUPS);

		userGroupRequest.setUserName("");

		UserGroupDAO userGroupResponse = uiWebService
				.userGroupOperation(userGroupRequest);

		object.getGroups().clear();
		object.getGroups().addAll(userGroupResponse.getGroups());

		userGroupRequest.setType(Constants.GET_USERS);

		userGroupRequest.setUserName("");

		userGroupResponse = uiWebService.userGroupOperation(userGroupRequest);

		object.getUsers().clear();
		object.getUsers().addAll(userGroupResponse.getUsers());
		
		object.setFilePath(null);

		return object;
	}

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public FolderManager getFolderManager() {
		return folderManager;
	}

	public void setFolderManager(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	public AgentFactory getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentFactory agentManager) {
		this.agentManager = agentManager;
	}

	public UiUtilityMethods getUiUtilityMethods() {
		return uiUtilityMethods;
	}

	public void setUiUtilityMethods(UiUtilityMethods uiUtilityMethods) {
		this.uiUtilityMethods = uiUtilityMethods;
	}
}
