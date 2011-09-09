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
package edu.unc.lib.dl.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.agents.GroupAgent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.service.UserManagementService;
import edu.unc.lib.dl.util.Constants;

/**
 * 
 * 
 */
public class UserManagementServiceImpl implements UserManagementService {
	protected final Log logger = LogFactory.getLog(getClass());

	private AgentManager agentManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#addUserToGroup(edu.unc.lib
	 * .dl.schema.UserGroupDAO)
	 */
	public UserGroupDAO addUserToGroup(UserGroupDAO request) {
		// TODO: call ?? to try to add user

		try {
			PID pid = new PID(request.getGroupPid());

			GroupAgent group = (GroupAgent) agentManager.getAgent(pid, false);

			request.setGroupName(group.getName());

			Agent user = agentManager.findPersonByOnyen(request.getAdminName(),
					true);

			pid = new PID(request.getPid());

			PersonAgent member = (PersonAgent) agentManager
					.getAgent(pid, false);

			request.setUserName(member.getName());

			agentManager.addMembership(group, member, user);

			request.setMessage(Constants.SUCCESS);
		} catch (IngestException e) {
			logger.debug("addMembership failed: ", e);
			request.setMessage(Constants.FAILURE);			
		} catch (NotFoundException e) {
			logger.debug("AddUserToGroup error: ", e);
			request.setMessage(Constants.FAILURE);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#createGroup(edu.unc.lib.
	 * dl.schema.UserGroupDAO)
	 */
	public UserGroupDAO createGroup(UserGroupDAO request) {
		logger.debug("ws create group: " + request.getGroupName());

		GroupAgent groupAgent = new GroupAgent(request.getGroupName());

		try {
			Agent agent = agentManager.findPersonByOnyen(
					request.getAdminName(), true);

			agentManager.addGroupAgent(groupAgent, agent, "Created through UI");

			request.setMessage(Constants.SUCCESS);
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);
		} catch (IngestException e) {
			request.setMessage(Constants.EXISTS);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#createUser(edu.unc.lib.dl
	 * .schema.UserGroupDAO)
	 */
	public UserGroupDAO createUser(UserGroupDAO request) {
		// TODO: call ?? to try to create user

		logger.debug("ws create user: " + request.getUserName());
		logger.debug("ws create onyen: " + request.getOnyen());

		try {
			Agent agent = agentManager.findPersonByOnyen(
					request.getAdminName(), true);

			PersonAgent person = new PersonAgent(request.getUserName(), request
					.getOnyen());

			agentManager.addPersonAgent(person, agent, "Created through UI");

			request.setMessage(Constants.SUCCESS);
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);
		} catch (IngestException e) {
			request.setMessage(Constants.EXISTS);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#deleteGroup(edu.unc.lib.
	 * dl.schema.UserGroupDAO)
	 */
	public UserGroupDAO deleteGroup(UserGroupDAO request) {
		try {
			Agent agent = agentManager.findPersonByOnyen(
					request.getAdminName(), true);

			PID pid = new PID(request.getPid());

			Agent group = agentManager.getAgent(pid, false);

			request.setGroupName(group.getName());

			agentManager.deleteAgent(group, agent);

			request.setMessage(Constants.SUCCESS);
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);
		} catch (IngestException e) {
			request.setMessage(Constants.EXISTS);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#deleteUser(edu.unc.lib.dl
	 * .schema.UserGroupDAO)
	 */
	public UserGroupDAO deleteUser(UserGroupDAO request) {
		try {
			Agent agent = agentManager.findPersonByOnyen(
					request.getAdminName(), true);

			PID pid = new PID(request.getPid());

			Agent person = agentManager.getAgent(pid, false);

			request.setUserName(person.getName());

			agentManager.deleteAgent(person, agent);

			request.setMessage(Constants.SUCCESS);
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);
		} catch (IngestException e) {
			request.setMessage(Constants.EXISTS);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#getGroups(java.lang.String)
	 */
	public UserGroupDAO getGroups(UserGroupDAO request) {

		// TODO: handle case of user specified (limit groups to those which have
		// the user as member)
		request.getGroups().clear();

		List<GroupAgent> groups = agentManager.getAllGroupAgents(false);

		Collections.sort(groups);

		int loop = groups.size();

		for (int i = 0; i < loop; i++) {
			edu.unc.lib.dl.schema.Agent agent = new edu.unc.lib.dl.schema.Agent();
			agent.setName(groups.get(i).getName());
			agent.setPid(groups.get(i).getPID().getPid());
			request.getGroups().add(agent);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#getUsers(java.lang.String)
	 */
	public UserGroupDAO getUsers(UserGroupDAO request) {
		int loop = 0;
		List<PersonAgent> users = new ArrayList<PersonAgent>(0);

		request.getUsers().clear();

		logger.debug(request.getType());

		if (Constants.GET_USERS.equals(request.getType())) {

			users = agentManager.getAllPersonAgents(false);
			Collections.sort(users);

		} else if (Constants.GET_USERS_IN_GROUP.equals(request.getType())) {

			PID pid = new PID(request.getGroupPid());

			users = agentManager.findGroupMembers(pid, false);

			Collections.sort(users);
		} else if (Constants.GET_USERS_OUTSIDE_GROUP.equals(request.getType())) {

			logger.debug(request.getGroupPid());

			List<PersonAgent> users0 = (List<PersonAgent>) agentManager
					.getAllPersonAgents(false);

			PID pid = new PID(request.getGroupPid());

			List<PersonAgent> users1 = (List<PersonAgent>) agentManager
					.findGroupMembers(pid, false);

			for (int i = 0; i < users1.size(); i++) {
				logger.debug("group members: "
						+ ((PersonAgent) users1.get(i)).getName());
			}
			
			users = (List<PersonAgent>) ListUtils.subtract(users0, users1);

			for (int i = 0; i < users.size(); i++) {
				logger.debug("non group members: " + users.get(i).getName());
			}

			Collections.sort(users);
		}

		loop = users.size();

		for (int i = 0; i < loop; i++) {
			edu.unc.lib.dl.schema.Agent agent = new edu.unc.lib.dl.schema.Agent();
			agent.setName(users.get(i).getName());
			agent.setPid(users.get(i).getPID().getPid());
			request.getUsers().add(agent);
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.unc.lib.dl.service.UserManagementService#removeUserFromGroup(edu.
	 * unc.lib.dl.schema.UserGroupDAO)
	 */
	public UserGroupDAO removeUserFromGroup(UserGroupDAO request) {
		try {
			PID pid = new PID(request.getGroupPid());

			GroupAgent group = (GroupAgent) agentManager.getAgent(pid, false);

			request.setGroupName(group.getName());

			Agent user = agentManager.findPersonByOnyen(request.getAdminName(),
					true);

			pid = new PID(request.getPid());

			PersonAgent member = (PersonAgent) agentManager
					.getAgent(pid, false);

			request.setUserName(member.getName());

			agentManager.removeMembership(group, member, user);

			request.setMessage(Constants.SUCCESS);
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return request;
	}

	public AgentManager getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentManager agentManager) {
		this.agentManager = agentManager;
	}

}
