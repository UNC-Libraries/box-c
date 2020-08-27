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
package edu.unc.lib.dl.model;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.PIDConstants;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.dl.util.URIUtil;

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
    public static final String ONYEN_DOMAIN = "ONYEN";

    /**
     * Get a PID object representing the given software agent
     *
     * @param agent software agent
     * @return PID for software agent
     */
    public static PID forSoftware(SoftwareAgent agent) {
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
        return PIDs.get(PIDConstants.AGENTS_QUALIFIER, SOFTWARE_AGENT_DOMAIN + "/" + URIUtil.toSlug(name));
    }

    /**
     * Get a PID object for a user with the provided username
     *
     * @param username username
     * @return PID for user
     */
    public static PID forPerson(String username) {
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
        return forPerson(princ.getUsername());
    }
}
