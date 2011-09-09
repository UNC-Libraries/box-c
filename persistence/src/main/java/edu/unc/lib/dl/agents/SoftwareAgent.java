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
 * Represents an agent that is actually a process or piece of software.
 */
public class SoftwareAgent extends AbstractAgent {
    // no extra methods currently defined. None may be necessary.

    // TODO add an optional release/build info file to Software Agents

    public SoftwareAgent() {
	super();
    }

    SoftwareAgent(PID pid, String name) {
	setPID(pid);
	setName(name);
    }
}
