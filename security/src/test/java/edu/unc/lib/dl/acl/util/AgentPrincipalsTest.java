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
package edu.unc.lib.dl.acl.util;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 *
 * @author bbpennel
 *
 */
public class AgentPrincipalsTest {

    private final static String USERNAME = "user";
    private final static String[] PRINCIPALS = new String[] {"grp1", "grp2"};
    private final static List<String> PRINCIPAL_LIST = Arrays.asList(PRINCIPALS);
    private final static AccessGroupSet PRINCIPAL_SET = new AccessGroupSet(PRINCIPALS);

    @Test
    public void testCreateFromThread() {
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(PRINCIPAL_SET);

        AgentPrincipals agent = AgentPrincipals.createFromThread();

        assertEquals(USERNAME, agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        GroupsThreadStore.clearStore();
    }

    @Test
    public void testWithUsername() {
        AgentPrincipals agent = new AgentPrincipals(USERNAME, PRINCIPAL_SET);

        assertEquals(USERNAME, agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        assertTrue(agent.getPrincipals().contains(USER_NAMESPACE + USERNAME));
    }

    @Test
    public void testWithoutUsername() {
        AgentPrincipals agent = new AgentPrincipals(null, PRINCIPAL_SET);

        assertNull(agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        assertFalse(agent.getPrincipals().contains(USER_NAMESPACE + USERNAME));
    }
}
