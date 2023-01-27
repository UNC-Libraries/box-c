package edu.unc.lib.boxc.model.fcrepo.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 * @author bbpennel
 */
public class AgentPidsTest {

    private static final String BASE_URI = "http://example.com/rest/";

    @BeforeAll
    public static void beforeAll() {
        TestHelper.setContentBase(BASE_URI);
        SoftwareAgentConstants.setCdrVersion("5.x");
    }

    @Test
    public void pidForSoftwareAgent() throws Exception {
        assertEquals(BASE_URI + "agents/software/bxc-services-5.x",
                AgentPids.forSoftware(SoftwareAgent.servicesAPI).getRepositoryPath());
        assertEquals("agents/software/bxc-services-5.x",
                AgentPids.forSoftware(SoftwareAgent.servicesAPI).getQualifiedId());
        assertEquals("software/bxc-services-5.x",
                AgentPids.forSoftware(SoftwareAgent.servicesAPI).getId());
    }

    @Test
    public void pidForNullSoftwareAgent() throws Exception {
        Assertions.assertThrows(NullPointerException.class, () -> {
            AgentPids.forSoftware((SoftwareAgent) null);
        });
    }

    @Test
    public void pidForSoftwareName() throws Exception {
        assertEquals(BASE_URI + "agents/software/useful-software",
                AgentPids.forSoftware("useful-software").getRepositoryPath());
    }

    @Test
    public void pidForNullSoftwareName() throws Exception {
        Assertions.assertThrows(NullPointerException.class, () -> {
            AgentPids.forSoftware((String) null);
        });
    }

    @Test
    public void pidForPersonUsername() throws Exception {
        assertEquals(BASE_URI + "agents/person/onyen/someuser",
                AgentPids.forPerson("someuser").getRepositoryPath());
        assertEquals("agents/person/onyen/someuser",
                AgentPids.forPerson("someuser").getQualifiedId());
        assertEquals("person/onyen/someuser",
                AgentPids.forPerson("someuser").getId());
    }

    @Test
    public void pidForNullPersonUsername() throws Exception {
        Assertions.assertThrows(NullPointerException.class, () -> {
            AgentPids.forPerson((String) null);
        });
    }

    @Test
    public void pidForPersonAgentPrincipals() throws Exception {
        AgentPrincipals princs = mock(AgentPrincipals.class);
        when(princs.getUsername()).thenReturn("someuser2");
        assertEquals(BASE_URI + "agents/person/onyen/someuser2",
                AgentPids.forPerson(princs).getRepositoryPath());
    }

    @Test
    public void pidForNullPersonAgentPrincipals() throws Exception {
        Assertions.assertThrows(NullPointerException.class, () -> {
            AgentPids.forPerson((AgentPrincipals) null);
        });
    }
}
