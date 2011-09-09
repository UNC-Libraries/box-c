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
 * Represents a Fedora object that may appear in PREMIS logs.
 */
public interface Agent extends Comparable<Agent> {

    /**
     * Gets the common (user displayable) name of this agent.
     *
     * @return a user-facing name for this agent.
     */
    public abstract String getName();

    /**
     * Gets the Fedora PID of this agent.
     *
     * @return the persistent identifier for the Fedora object.
     */
    public abstract PID getPID();

}
