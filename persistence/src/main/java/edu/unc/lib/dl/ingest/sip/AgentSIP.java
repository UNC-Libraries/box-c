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
package edu.unc.lib.dl.ingest.sip;

import java.util.List;

import edu.unc.lib.dl.agents.Agent;

public class AgentSIP implements SubmissionInformationPackage {
	// private static String containerPath = "/admin/people";
	private List<Agent> agents = null;

	public List<Agent> getAgents() {
		return agents;
	}

	public void setAgents(List<Agent> agents) {
		this.agents = agents;
	}

	@Override
	public Agent getOwner() {
		return null;
	}

	@Override
	public Agent getDepositor() {
		return null;
	}
}
