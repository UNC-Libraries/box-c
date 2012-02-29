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
package edu.unc.lib.dl.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.agents.GroupAgent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.agents.SoftwareAgent;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.AgentSIP;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;

public class AgentManager extends AgentFactory {
	private static final Log log = LogFactory.getLog(AgentManager.class);

	private DigitalObjectManager digitalObjectManager = null;

	/**
	 * This add a person agent object to Fedora and returning the new
	 * PersonAgent.
	 * 
	 * @param name
	 *            name of the person
	 * @param apim
	 *            fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public GroupAgent addGroupAgent(GroupAgent group, Agent user, String message)
			throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		GroupAgent conflict = this.findGroupByName(group.getName(), false);
		if (conflict != null) {
			throw new IngestException("A group named '" + group.getName()
					+ "' already exists: " + conflict.getPID());
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(group);
		sip.setAgents(addlist);

		DepositRecord record = new DepositRecord(user, DepositMethod.Unspecified);
		IngestResult resp = this.getDigitalObjectManager().addWhileBlocking(sip, record);		
		log.debug("response is:" + resp);
		log.debug("response size:" + resp.derivedPIDs.size());
		PID newgrouppid = resp.derivedPIDs.iterator().next();
		return this.getGroupAgents(Collections.singletonList(newgrouppid), false).get(
				0);
	}

	/**
	 * Adds a member to a group.
	 * 
	 * @param group
	 *            the GroupAgent
	 * @param member
	 *            the member PersonAgent
	 * @throws NotFoundException
	 *             if either object are not found in the repository
	 */
	public void addMembership(GroupAgent group, PersonAgent member, Agent user)
			throws NotFoundException, IngestException {
		this.getDigitalObjectManager().addRelationship(group.getPID(),
				ContentModelHelper.Relationship.member, member.getPID());
	}

	/**
	 * This add a person agent object to Fedora and returning the new
	 * PersonAgent.
	 * 
	 * @param name
	 *            name of the person
	 * @param apim
	 *            fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public PersonAgent addPersonAgent(PersonAgent person, Agent user,
			String message) throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		PersonAgent conflict = this.findPersonByName(person.getName(), false);
		if (conflict != null) {
			throw new IngestException("A person named '" + person.getName()
					+ "' already exists: " + conflict.getPID());
		}

		// check for duplicates by onyen
		conflict = this.findPersonByOnyen(person.getOnyen(), false);
		if (conflict != null) {
			throw new IngestException("A person with the Onyen '"
					+ person.getOnyen() + "' already exists: "
					+ conflict.getOnyen() + ", " + conflict.getPID());
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(person);
		sip.setAgents(addlist);

		DepositRecord record = new DepositRecord(user, DepositMethod.Unspecified);
		IngestResult resp = this.getDigitalObjectManager().addWhileBlocking(sip, record);
		log.debug("response is:" + resp);
		return this.getPersonAgents(Collections.singletonList(resp.derivedPIDs.iterator().next()), false)
				.get(0);
	}

	/**
	 * This add a person agent object to Fedora and returning the new
	 * PersonAgent.
	 * 
	 * @param name
	 *            name of the person
	 * @param apim
	 *            fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public SoftwareAgent addSoftwareAgent(SoftwareAgent software, Agent user,
			String message) throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		SoftwareAgent conflict = this.findSoftwareByName(software.getName());
		if (conflict != null) {
			throw new IngestException("A software agent named '"
					+ software.getName() + "' already exists: "
					+ conflict.getPID());
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(software);
		sip.setAgents(addlist);

		DepositRecord record = new DepositRecord(user, DepositMethod.Unspecified);
		IngestResult resp = this.getDigitalObjectManager().addWhileBlocking(sip, record);
		log.debug("response is:" + resp);
		return this.getSoftwareAgents(Collections.singletonList(resp.derivedPIDs.iterator().next())).get(0);
	}

	public void deleteAgent(Agent agent, Agent user) throws IngestException,
			NotFoundException {
		// if they own *anything* then throw exception
		ArrayList<PID> pids = new ArrayList<PID>();
		pids.add(agent.getPID());
		if (agent instanceof PersonAgent) {
			// make sure we can find the agent
			List<PersonAgent> peopleList = this.getPersonAgents(pids, true);
			if (peopleList.size() < 1) {
				throw new NotFoundException(
						"Cannot find the object to delete: " + agent.getPID());
			}
			// remove memberships
			// PersonAgent person = peopleList.get(0);
			// for (GroupAgent group : person.getGroups()) {
			// try {
			// this.removeMembership(group, person, user);
			// } catch (NotFoundException e) {
			// log.error("Unexpected error finding known group membership.", e);
			// throw new Error("Unexpected error finding known group
			// membership.", e);
			// }
			// }
			// } else if (agent instanceof SoftwareAgent) {
			// make sure we can find the agent
			// remove any rels??
		} else if (agent instanceof GroupAgent) {
			// make sure we can find the agent
			List<GroupAgent> groups = this.getGroupAgents(pids, false);
			if (groups.size() < 1) {
				throw new NotFoundException("Cannot find the group to delete: "
						+ agent.getPID());
			}
			// remove any rels??
		} else if (agent instanceof SoftwareAgent) {
			// make sure we can find the agent
			List<SoftwareAgent> groups = this.getSoftwareAgents(pids);
			if (groups.size() < 1) {
				throw new NotFoundException(
						"Cannot find the software agent to delete: "
								+ agent.getPID());
			}
			// remove any rels??
		} else {
			throw new UnsupportedOperationException(
					"Don't know how to delete that kind of agent:"
							+ agent.getName() + " " + agent.getPID());
		}
		this.getDigitalObjectManager().delete(agent.getPID(),
				AgentManager.getAdministrativeGroupAgentStub(),
				"Deleting agent through AgentManager");
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	/**
	 * Removes a member to a group.
	 * 
	 * @param group
	 *            the GroupAgent
	 * @param member
	 *            the member PersonAgent
	 * @throws NotFoundException
	 *             if either object are not found in the repository
	 */
	public void removeMembership(GroupAgent group, PersonAgent member,
			Agent user) throws NotFoundException, IngestException {
		this.getDigitalObjectManager().purgeRelationship(group.getPID(),
				ContentModelHelper.Relationship.member, member.getPID());
	}

	public void setDigitalObjectManager(
			DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}
}
