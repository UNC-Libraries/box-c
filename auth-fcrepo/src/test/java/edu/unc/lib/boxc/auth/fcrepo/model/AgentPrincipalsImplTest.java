package edu.unc.lib.boxc.auth.fcrepo.model;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;

/**
 *
 * @author bbpennel
 *
 */
public class AgentPrincipalsImplTest {

    private final static String USERNAME = "user";
    private final static String[] PRINCIPALS = new String[] {"grp1", "grp2"};
    private final static List<String> PRINCIPAL_LIST = Arrays.asList(PRINCIPALS);
    private final static AccessGroupSet PRINCIPAL_SET = new AccessGroupSetImpl(PRINCIPALS);

    @Test
    public void testCreateFromThread() {
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(PRINCIPAL_SET);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();

        assertEquals(USERNAME, agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        GroupsThreadStore.clearStore();
    }

    @Test
    public void testWithUsername() {
        AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, PRINCIPAL_SET);

        assertEquals(USERNAME, agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        assertTrue(agent.getPrincipals().contains(USER_NAMESPACE + USERNAME));
    }

    @Test
    public void testWithoutUsername() {
        AgentPrincipals agent = new AgentPrincipalsImpl(null, PRINCIPAL_SET);

        assertNull(agent.getUsername());
        assertTrue(agent.getPrincipals().containsAll(PRINCIPAL_LIST));

        assertFalse(agent.getPrincipals().contains(USER_NAMESPACE + USERNAME));
    }
}
