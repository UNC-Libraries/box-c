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
package edu.unc.lib.dl.agents;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.AgentSIP;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class AgentManager {
	private static final Log log = LogFactory.getLog(AgentManager.class);

	public static GroupAgent getAdministrativeGroupAgentStub() {
		GroupAgent result = new GroupAgent("Administrator Group");
		result.setPID(ContentModelHelper.Administrative_PID.ADMINISTRATOR_GROUP.getPID());
		return result;
	}

	public static SoftwareAgent getRepositorySoftwareAgentStub() {
		SoftwareAgent result = new SoftwareAgent(
				ContentModelHelper.Administrative_PID.REPOSITORY_MANAGEMENT_SOFTWARE.getPID(),
				"Repository Management Software");
		return result;
	}

	private DigitalObjectManager digitalObjectManager = null;

	private TripleStoreQueryService tripleStoreQueryService = null;

	/**
	 * This add a person agent object to Fedora and returning the new PersonAgent.
	 * 
	 * @param name
	 *           name of the person
	 * @param apim
	 *           fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public GroupAgent addGroupAgent(GroupAgent group, Agent user, String message) throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		try {
			GroupAgent conflict = this.findGroupByName(group.getName(), false);
			throw new IngestException("A group named '" + group.getName() + "' already exists: " + conflict.getPID());
		} catch (NotFoundException expected) {
			// this exception is expected, if there are no duplicates we
			// proceed.
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(group);
		sip.setAgents(addlist);

		PID resp = this.getDigitalObjectManager().addSingleObject(sip, user, message);
		log.debug("response is:" + resp);
		return this.getGroupAgents(Collections.singletonList(resp), false).get(0);
	}

	/**
	 * Adds a member to a group.
	 * 
	 * @param group
	 *           the GroupAgent
	 * @param member
	 *           the member PersonAgent
	 * @throws NotFoundException
	 *            if either object are not found in the repository
	 */
	public void addMembership(GroupAgent group, PersonAgent member, Agent user) throws NotFoundException,
			IngestException {
		this.getDigitalObjectManager().addRelationship(group.getPID(), ContentModelHelper.Relationship.member,
				member.getPID());
	}

	/**
	 * This add a person agent object to Fedora and returning the new PersonAgent.
	 * 
	 * @param name
	 *           name of the person
	 * @param apim
	 *           fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public PersonAgent addPersonAgent(PersonAgent person, Agent user, String message) throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		PersonAgent conflict = null;
		try {
			conflict = this.findPersonByName(person.getName(), false);
		} catch (NotFoundException expected) {
		}
		if (conflict != null) {
			throw new IngestException("A person named '" + person.getName() + "' already exists: " + conflict.getPID());
		}

		// check for duplicates by onyen
		conflict = null;
		try {
			conflict = this.findPersonByOnyen(person.getOnyen(), false);
		} catch (NotFoundException expected) {
		}
		if (conflict != null) {
			throw new IngestException("A person with the Onyen '" + person.getOnyen() + "' already exists: "
					+ conflict.getOnyen() + ", " + conflict.getPID());
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(person);
		sip.setAgents(addlist);

		PID resp = this.getDigitalObjectManager().addSingleObject(sip, user, message);
		log.debug("response is:" + resp);
		return this.getPersonAgents(Collections.singletonList(resp), false).get(0);
	}

	/**
	 * This add a person agent object to Fedora and returning the new PersonAgent.
	 * 
	 * @param name
	 *           name of the person
	 * @param apim
	 *           fedora mgt. client
	 * @return a person agent
	 * @throws IngestException
	 */
	public SoftwareAgent addSoftwareAgent(SoftwareAgent software, Agent user, String message) throws IngestException {
		AgentSIP sip = new AgentSIP();

		// check for duplicates by name
		try {
			SoftwareAgent conflict = this.findSoftwareByName(software.getName());
			throw new IngestException("A software agent named '" + software.getName() + "' already exists: "
					+ conflict.getPID());
		} catch (NotFoundException expected) {
			// this exception is expected, if there are no duplicates we
			// proceed.
		}

		List<Agent> addlist = new ArrayList<Agent>();
		addlist.add(software);
		sip.setAgents(addlist);

		PID resp = this.getDigitalObjectManager().addSingleObject(sip, user, message);
		log.debug("response is:" + resp);
		return this.getSoftwareAgents(Collections.singletonList(resp)).get(0);
	}

	public void deleteAgent(Agent agent, Agent user) throws IngestException, NotFoundException {
		// if they own *anything* then throw exception
		ArrayList<PID> pids = new ArrayList<PID>();
		pids.add(agent.getPID());
		if (agent instanceof PersonAgent) {
			// make sure we can find the agent
			List<PersonAgent> peopleList = this.getPersonAgents(pids, true);
			if (peopleList.size() < 1) {
				throw new NotFoundException("Cannot find the object to delete: " + agent.getPID());
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
				throw new NotFoundException("Cannot find the group to delete: " + agent.getPID());
			}
			// remove any rels??
		} else if (agent instanceof SoftwareAgent) {
			// make sure we can find the agent
			List<SoftwareAgent> groups = this.getSoftwareAgents(pids);
			if (groups.size() < 1) {
				throw new NotFoundException("Cannot find the software agent to delete: " + agent.getPID());
			}
			// remove any rels??
		} else {
			throw new UnsupportedOperationException("Don't know how to delete that kind of agent:" + agent.getName() + " "
					+ agent.getPID());
		}
		this.getDigitalObjectManager().delete(agent.getPID(), AgentManager.getAdministrativeGroupAgentStub(),
				"Deleting agent through AgentManager");
	}

	private List<GroupAgent> findGroupAgents(String whereClause, boolean loadMembership) {
		Map<PID, GroupAgent> map = new HashMap<PID, GroupAgent>();
		StringBuffer q = new StringBuffer();
		q.append("select $pid $label from <%1$s>").append(" where ");
		q.append(whereClause);
		q.append(" and $pid <%2$s> $label");
		q.append(" and $pid <%3$s> <%4$s>;");
		String query = String.format(q.toString(), this.getTripleStoreQueryService().getResourceIndexModelUri(),
				ContentModelHelper.FedoraProperty.label, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.GROUPAGENT);
		List<List<String>> resp = this.getTripleStoreQueryService().queryResourceIndex(query);

		for (List<String> solution : resp) {
			// new group record
			PID pid = new PID(solution.get(0));
			GroupAgent current = new GroupAgent(pid, solution.get(1));
			map.put(pid, current);
		}
		if (loadMembership && map.size() > 0) {
			this.loadGroupMembers(map);
		}
		List<GroupAgent> result = new ArrayList<GroupAgent>();
		result.addAll(map.values());
		return result;
	}

	public GroupAgent findGroupByName(String name, boolean loadMembership) throws NotFoundException {
		String whereClause = String.format(" $pid <%1$s> '%2$s' ", ContentModelHelper.FedoraProperty.label, name);
		List<GroupAgent> resp = this.findGroupAgents(whereClause, loadMembership);
		if (resp.size() > 1) {
			throw new IllegalRepositoryStateException("There cannot be more than one group with the same name.");
		} else if (resp.size() == 0) {
			throw new NotFoundException("No group found by the name: " + name);
		} else {
			return resp.get(0);
		}
	}

	public List<PersonAgent> findGroupMembers(PID groupPID, boolean loadGroupMembership) {
		String whereClause = String.format(" <info:fedora/%1$s> <%2$s> $pid ", groupPID.getPid(),
				ContentModelHelper.Relationship.member);
		return this.findPersonAgents(whereClause, loadGroupMembership);
	}

	private List<PersonAgent> findPersonAgents(String whereClause, boolean loadGroupMembership) {
		Map<PID, PersonAgent> map = new HashMap<PID, PersonAgent>();
		StringBuffer q = new StringBuffer();
		q.append("select $pid $label $onyen from <%1$s>").append(" where ");
		q.append(whereClause);
		q.append(" and $pid <%2$s> $label");
		q.append(" and $pid <%3$s> $onyen");
		q.append(" and $pid <%4$s> <%5$s>;");
		String query = String.format(q.toString(), this.getTripleStoreQueryService().getResourceIndexModelUri(),
				ContentModelHelper.FedoraProperty.label, ContentModelHelper.CDRProperty.onyen,
				ContentModelHelper.FedoraProperty.hasModel, ContentModelHelper.Model.PERSONAGENT);
		List<List<String>> resp = this.getTripleStoreQueryService().queryResourceIndex(query);

		for (List<String> solution : resp) {
			// new person record
			PID pid = new PID(solution.get(0));
			PersonAgent current = new PersonAgent(pid, solution.get(1), solution.get(2));
			map.put(pid, current);
		}
		if (loadGroupMembership && map.size() > 0) {
			this.loadPersonMemberships(map);
		}
		List<PersonAgent> result = new ArrayList<PersonAgent>();
		result.addAll(map.values());
		return result;
	}

	public PersonAgent findPersonByName(String name, boolean loadGroupMembership) throws NotFoundException {
		String whereClause = String.format(" $pid <%1$s> '%2$s' ", ContentModelHelper.FedoraProperty.label, name);
		List<PersonAgent> resp = this.findPersonAgents(whereClause, loadGroupMembership);
		if (resp == null || resp.size() == 0) {
			throw new NotFoundException("No person found by the name: " + name);
		} else if (resp.size() > 1) {
			throw new IllegalRepositoryStateException("There cannot be more than one person with the same name.");
		} else {
			return resp.get(0);
		}
	}

	public PersonAgent findPersonByOnyen(String onyen, boolean loadGroupMembership) throws NotFoundException {
		String whereClause = String.format(" $pid <%1$s> '%2$s' ", ContentModelHelper.CDRProperty.onyen, onyen);
		List<PersonAgent> resp = this.findPersonAgents(whereClause, loadGroupMembership);
		if (resp == null || resp.size() == 0) {
			throw new NotFoundException("No person found with the onyen: " + onyen);
		} else if (resp.size() > 1) {
			throw new IllegalRepositoryStateException("There cannot be more than one person with the same onyen.");
		} else {
			return resp.get(0);
		}
	}

	private List<SoftwareAgent> findSoftwareAgents(String whereClause) {
		List<SoftwareAgent> result = new ArrayList<SoftwareAgent>();
		StringBuffer q = new StringBuffer();
		q.append("select $pid $label from <%1$s>").append(" where ");
		q.append(whereClause);
		q.append(" and $pid <%2$s> $label");
		q.append(" and $pid <%3$s> <%4$s>;");
		String query = String.format(q.toString(), this.getTripleStoreQueryService().getResourceIndexModelUri(),
				ContentModelHelper.FedoraProperty.label, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.SOFTWAREAGENT);
		List<List<String>> resp = this.getTripleStoreQueryService().queryResourceIndex(query);

		for (List<String> solution : resp) {
			// new group record
			PID pid = new PID(solution.get(0));
			SoftwareAgent current = new SoftwareAgent(pid, solution.get(1));
			result.add(current);
		}
		return result;
	}

	public SoftwareAgent findSoftwareByName(String name) throws NotFoundException {
		String whereClause = String.format(" $pid <%1$s> '%2$s' ", ContentModelHelper.FedoraProperty.label, name);
		List<SoftwareAgent> resp = this.findSoftwareAgents(whereClause);
		if (resp.size() > 1) {
			throw new IllegalRepositoryStateException("There cannot be more than one group with the same name.");
		} else if (resp.size() == 0) {
			throw new NotFoundException("No group found by the name: " + name);
		} else {
			return resp.get(0);
		}
	}

	public Agent getAgent(PID pid, boolean loadMembership) throws NotFoundException {
		List<URI> types = this.getTripleStoreQueryService().lookupContentModels(pid);
		List<PID> list = new ArrayList<PID>();
		Agent result = null;
		list.add(pid);
		try {
			if (types.contains(ContentModelHelper.Model.PERSONAGENT.getURI())) {
				result = this.getPersonAgents(list, loadMembership).get(0);
			} else if (types.contains(ContentModelHelper.Model.GROUPAGENT.getURI())) {
				result = this.getGroupAgents(list, loadMembership).get(0);
			} else if (types.contains(ContentModelHelper.Model.SOFTWAREAGENT.getURI())) {
				result = this.getSoftwareAgents(list).get(0);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new NotFoundException("No agent was found for the pid " + pid);
		}
		return result;
	}

	public List<GroupAgent> getAllGroupAgents(boolean loadMembership) {
		String where = " $foo <mulgara:is> 'foo'";
		return this.findGroupAgents(where, loadMembership);
	}

	public List<PersonAgent> getAllPersonAgents(boolean loadGroupMembership) {
		String where = " $foo <mulgara:is> 'foo'";
		return this.findPersonAgents(where, loadGroupMembership);
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public List<GroupAgent> getGroupAgents(List<PID> pids, boolean loadMembership) {
		StringBuffer q = new StringBuffer();
		boolean first = true;
		if (pids.size() > 1) {
			q.append(" ( ");
		}
		for (PID pid : pids) {
			if (first) {
				first = false;
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			} else {
				q.append(" or");
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			}
		}
		if (pids.size() > 1) {
			q.append(" )");
		}
		return this.findGroupAgents(q.toString(), loadMembership);
	}

	public List<PersonAgent> getPersonAgents(List<PID> pids, boolean loadGroupMembership) {
		StringBuffer q = new StringBuffer();
		boolean first = true;
		if (pids.size() > 1) {
			q.append(" ( ");
		}
		for (PID pid : pids) {
			if (first) {
				first = false;
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			} else {
				q.append(" or");
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			}
		}
		if (pids.size() > 1) {
			q.append(" )");
		}
		return this.findPersonAgents(q.toString(), loadGroupMembership);
	}

	public List<SoftwareAgent> getSoftwareAgents(List<PID> pids) {
		StringBuffer q = new StringBuffer();
		boolean first = true;
		if (pids.size() > 1) {
			q.append(" ( ");
		}
		for (PID pid : pids) {
			if (first) {
				first = false;
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			} else {
				q.append(" or");
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			}
		}
		if (pids.size() > 1) {
			q.append(" )");
		}
		return this.findSoftwareAgents(q.toString());
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	private void loadGroupMembers(Map<PID, GroupAgent> pid2group) {
		StringBuffer q = new StringBuffer();
		q.append("select $pid $ppid $plabel $ponyen from <%1$s>");
		q.append(" where (");
		boolean first = true;
		for (PID pid : pid2group.keySet()) {
			if (first) {
				first = false;
				q.append(" $pid <mulgara:is> <").append(pid.getURI()).append(">");
			} else {
				q.append(" or");
				q.append(" $pid <mulgara:is> <").append(pid.getURI()).append(">");
			}
		}
		q.append(")");
		q.append(" and $pid <%2$s> $ppid");
		q.append(" and $ppid <%3$s> $plabel");
		q.append(" and $ppid <%4$s> $ponyen;");
		String query = String.format(q.toString(), this.getTripleStoreQueryService().getResourceIndexModelUri(),
				ContentModelHelper.Relationship.member, ContentModelHelper.FedoraProperty.label,
				ContentModelHelper.CDRProperty.onyen);
		List<List<String>> resp = this.getTripleStoreQueryService().queryResourceIndex(query);
		for (List<String> solution : resp) {
			// new group membership solution
			PID gpid = new PID(solution.get(0));
			PID ppid = new PID(solution.get(1));
			String plabel = solution.get(2);
			String ponyen = solution.get(3);
			PersonAgent p = new PersonAgent(ppid, plabel, ponyen);
			pid2group.get(gpid).addMember(p);
		}
	}

	private void loadPersonMemberships(Map<PID, PersonAgent> pid2person) {
		StringBuffer q = new StringBuffer();
		q.append("select $pid $grouppid $grouplabel from <%1$s>");
		q.append(" where (");
		boolean first = true;
		for (PID pid : pid2person.keySet()) {
			if (first) {
				first = false;
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			} else {
				q.append(" or");
				q.append(" $pid <mulgara:is> <info:fedora/").append(pid.getPid()).append(">");
			}
		}
		q.append(")");
		q.append(" and $grouppid <%2$s> $pid");
		q.append(" and $grouppid <%3$s> $grouplabel;");
		String query = String.format(q.toString(), this.getTripleStoreQueryService().getResourceIndexModelUri(),
				ContentModelHelper.Relationship.member, ContentModelHelper.FedoraProperty.label);
		List<List<String>> resp = this.getTripleStoreQueryService().queryResourceIndex(query);
		for (List<String> solution : resp) {
			// new group membership solution
			PID pid = new PID(solution.get(0));
			PID gpid = new PID(solution.get(1));
			String glabel = solution.get(2);
			GroupAgent g = new GroupAgent(gpid, glabel);
			pid2person.get(pid).addGroup(g);
		}
	}

	/**
	 * Removes a member to a group.
	 * 
	 * @param group
	 *           the GroupAgent
	 * @param member
	 *           the member PersonAgent
	 * @throws NotFoundException
	 *            if either object are not found in the repository
	 */
	public void removeMembership(GroupAgent group, PersonAgent member, Agent user) throws NotFoundException,
			IngestException {
		this.getDigitalObjectManager().purgeRelationship(group.getPID(), ContentModelHelper.Relationship.member,
				member.getPID());
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
