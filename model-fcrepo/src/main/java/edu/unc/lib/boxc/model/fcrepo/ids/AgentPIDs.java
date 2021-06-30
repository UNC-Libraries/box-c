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
package edu.unc.lib.boxc.model.fcrepo.ids;

import java.util.Objects;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.objects.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.dl.acl.util.AgentPrincipals;

/**
 * Helper methods for getting PIDs for agent
 *
 * @author bbpennel
 */
public class AgentPids {

    private AgentPids() {
    }

    public static final String SOFTWARE_AGENT_DOMAIN = "software";
    public static final String PERSON_AGENT_DOMAIN = "person";
    public static final String ONYEN_DOMAIN = "onyen";

    /**
     * Get a PID object representing the given software agent
     *
     * @param agent software agent
     * @return PID for software agent
     */
    public static PID forSoftware(SoftwareAgent agent) {
        Objects.requireNonNull(agent, "Agent must not be null");
        return PIDs.get(PIDConstants.AGENTS_QUALIFIER, SOFTWARE_AGENT_DOMAIN + "/" + agent.getFullname());
    }

    /**
     * It is strongly recommended to use the forSoftware(SoftwareAgent agent) version of this method.
     * Get a PID object representing the software agent for the provided name
     *
     * @param name name of the software agent
     * @return PID for the software agent
     */
    public static PID forSoftware(String name) {
        Objects.requireNonNull(name, "Agent name must not be null");
        return PIDs.get(PIDConstants.AGENTS_QUALIFIER, SOFTWARE_AGENT_DOMAIN + "/" + URIUtil.toSlug(name));
    }

    /**
     * Get a PID object for a user with the provided username
     *
     * @param username username
     * @return PID for user
     */
    public static PID forPerson(String username) {
        Objects.requireNonNull(username, "Username must not be null");
        return PIDs.get(PIDConstants.AGENTS_QUALIFIER, PERSON_AGENT_DOMAIN
                + "/" + ONYEN_DOMAIN + "/" + URIUtil.toSlug(username));
    }

    /**
     * Get a PID object for a user based on the provided agent principals
     *
     * @param princ agent principals
     * @return PID object for user
     */
    public static PID forPerson(AgentPrincipals princ) {
        Objects.requireNonNull(princ, "Agent principals must not be null");
        return forPerson(princ.getUsername());
    }
}
