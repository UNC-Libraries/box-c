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

import edu.unc.lib.dl.fedora.PID;

/**
 * Abstract implementation of the {@link Agent} interface.
 * 
 */
public abstract class AbstractAgent implements Agent, Comparable<Agent> {

	private String _name;

	private PID _pid;

	@Override
	public int compareTo(Agent obj) {
		if (this.getName() != null) {
			return this.getName().compareTo(obj.getName());
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if (o instanceof Agent) {
			if (this.getPID() != null) {
				result = this.getPID().equals(((Agent) o).getPID());
			}
		}
		return result;
	}

	/**
	 * @inheritDoc
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @inheritDoc
	 */
	public PID getPID() {
		return _pid;
	}

	void setName(final String name) {
		this._name = name;
	}

	void setPID(PID pid) {
		this._pid = pid;
	}
}
