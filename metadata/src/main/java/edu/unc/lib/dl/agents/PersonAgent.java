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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.unc.lib.dl.fedora.PID;

/**
 * Represents an {@link Agent} that is some person who interacts with the repository.
 */
public class PersonAgent extends AbstractAgent {
	// private Set<String> roles = new HashSet<String>();
	private Set<GroupAgent> groups = new HashSet<GroupAgent>();

	private String onyen = null;

	public PersonAgent(PID pid, String name, String onyen) {
		this.setName(name);
		this.setPID(pid);
		this.setOnyen(onyen);
	}

	public PersonAgent(String name, String onyen) {
		this.setName(name);
		this.setOnyen(onyen);
	}

	/**
	 * Gets the roles played by this person in the repository. This may or may not be advisable to keep around ...
	 *
	 * @return this person's roles
	 */
	// public Set<String> getRoles() {
	// return Collections.unmodifiableSet(_roles);
	// }
	void addGroup(GroupAgent group) {
		this.groups.add(group);
	}

	/**
	 * Sets this person's roles.
	 *
	 * @param roles
	 *           a set of roles played by this person in the repository.
	 */
	// void setRoles(final Set<String> roles) {
	// _roles.addAll(roles);
	// }
	public Set<GroupAgent> getGroups() {
		return Collections.unmodifiableSet(groups);
	}

	public String getOnyen() {
		return onyen;
	}

	void setGroups(Set<GroupAgent> groups) {
		this.groups = groups;
	}

	void setOnyen(String onyen) {
		this.onyen = onyen;
	}
}
