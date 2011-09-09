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
package edu.unc.lib.dl.ws;

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.service.UserManagementService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class UserGroupEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());

	private UserManagementService userManagementService;

	@PayloadRoot(localPart = Constants.USER_GROUP_DAO, namespace = Constants.NAMESPACE)
	public UserGroupDAO userOrGroupRequest(UserGroupDAO request) {

		if (request.getType().equals(Constants.CREATE_GROUP)) {
			return userManagementService.createGroup(request);
		} else if (request.getType().equals(Constants.CREATE_USER)) {
			return userManagementService.createUser(request);
		} else if (request.getType().equals(Constants.DELETE_GROUP)) {
			return userManagementService.deleteGroup(request);
		} else if (request.getType().equals(Constants.DELETE_USER)) {
			return userManagementService.deleteUser(request);
		} else if (request.getType().equals(Constants.ADD_USER_TO_GROUP)) {
			return userManagementService.addUserToGroup(request);
		} else if (request.getType().equals(Constants.REMOVE_USER_FROM_GROUP)) {
			return userManagementService.removeUserFromGroup(request);
		} else if (request.getType().equals(Constants.GET_USERS)) {
			return userManagementService.getUsers(request);
		} else if (request.getType().equals(Constants.GET_USERS_IN_GROUP)) {
			return userManagementService.getUsers(request);
		} else if (request.getType().equals(Constants.GET_USERS_OUTSIDE_GROUP)) {
			return userManagementService.getUsers(request);
		} else if (request.getType().equals(Constants.GET_GROUPS)) {
			return userManagementService.getGroups(request);
		}

		// Something bad happened, not sure what
		request.setMessage(Constants.FAILURE);
		return request;
	}

	public void setUserManagementService(
			UserManagementService userManagementService) {
		this.userManagementService = userManagementService;
	}

}