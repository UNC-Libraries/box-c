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

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.unc.lib.boxc.model.fcrepo.ids.AgentPIDs;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.SoftwareAgentConstants;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * @author bbpennel
 */
public class AgentPIDsTest {

    private static final String BASE_URI = "http://example.com/rest/";

    @BeforeClass
    public static void beforeClass() {
        TestHelper.setContentBase(BASE_URI);
        SoftwareAgentConstants.setCdrVersion("5.x");
    }

    @Test
    public void pidForSoftwareAgent() throws Exception {
        assertEquals(BASE_URI + "agents/software/bxc-services-5.x",
                AgentPIDs.forSoftware(SoftwareAgent.servicesAPI).getRepositoryPath());
        assertEquals("agents/software/bxc-services-5.x",
                AgentPIDs.forSoftware(SoftwareAgent.servicesAPI).getQualifiedId());
        assertEquals("software/bxc-services-5.x",
                AgentPIDs.forSoftware(SoftwareAgent.servicesAPI).getId());
    }

    @Test(expected = NullPointerException.class)
    public void pidForNullSoftwareAgent() throws Exception {
        AgentPIDs.forSoftware((SoftwareAgent) null);
    }

    @Test
    public void pidForSoftwareName() throws Exception {
        assertEquals(BASE_URI + "agents/software/useful-software",
                AgentPIDs.forSoftware("useful-software").getRepositoryPath());
    }

    @Test(expected = NullPointerException.class)
    public void pidForNullSoftwareName() throws Exception {
        AgentPIDs.forSoftware((String) null);
    }

    @Test
    public void pidForPersonUsername() throws Exception {
        assertEquals(BASE_URI + "agents/person/onyen/someuser",
                AgentPIDs.forPerson("someuser").getRepositoryPath());
        assertEquals("agents/person/onyen/someuser",
                AgentPIDs.forPerson("someuser").getQualifiedId());
        assertEquals("person/onyen/someuser",
                AgentPIDs.forPerson("someuser").getId());
    }

    @Test(expected = NullPointerException.class)
    public void pidForNullPersonUsername() throws Exception {
        AgentPIDs.forPerson((String) null);
    }

    @Test
    public void pidForPersonAgentPrincipals() throws Exception {
        AgentPrincipals princs = new AgentPrincipals("someuser2", new AccessGroupSet("group"));
        assertEquals(BASE_URI + "agents/person/onyen/someuser2",
                AgentPIDs.forPerson(princs).getRepositoryPath());
    }

    @Test(expected = NullPointerException.class)
    public void pidForNullPersonAgentPrincipals() throws Exception {
        AgentPIDs.forPerson((AgentPrincipals) null);
    }
}
