package edu.unc.lib.boxc.services.camel.patronAccess;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.camel.BeanInject;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessDetails;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessOperationSender;

/**
 * @author bbpennel
 */
public class PatronAccessAssignmentRouterTest extends CamelSpringTestSupport {
    private static final String USER = "someone";
    private static final String PRINCIPALS = "my:special:group;everyone;authenticated";

    @BeanInject(value = "patronAccessAssignmentService")
    private PatronAccessAssignmentService patronAccessAssignmentService;

    @BeanInject(value = "patronAccessOperationSender")
    private PatronAccessOperationSender patronAccessOperationSender;

    @Captor
    private ArgumentCaptor<PatronAccessAssignmentRequest> requestCaptor;

    @Before
    public void init() throws Exception {
        initMocks(this);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("spring-test/jms-context.xml", "/patron-access-test-context.xml");
    }

    @Test
    public void validMessageTest() throws Exception {
        AgentPrincipals agent = new AgentPrincipalsImpl(USER, new AccessGroupSetImpl(PRINCIPALS));
        PID pid = PIDs.get(UUID.randomUUID().toString());
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        PatronAccessAssignmentRequest request = new PatronAccessAssignmentRequest(agent, pid, accessDetails);
        patronAccessOperationSender.sendUpdateRequest(request);

        verify(patronAccessAssignmentService, timeout(1000)).updatePatronAccess(requestCaptor.capture());
        PatronAccessAssignmentRequest received = requestCaptor.getValue();

        assertEquals(pid, received.getTargetPid());
        assertEquals(agent.getPrincipals(), received.getAgent().getPrincipals());
        assertEquals(accessDetails.getRoles(), received.getAccessDetails().getRoles());
    }

    @Test
    public void insufficientPermissionsTest() throws Exception {
        AgentPrincipals agent = new AgentPrincipalsImpl(USER, new AccessGroupSetImpl(PRINCIPALS));
        PID pid = PIDs.get(UUID.randomUUID().toString());
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        when(patronAccessAssignmentService.updatePatronAccess(any(PatronAccessAssignmentRequest.class)))
                .thenThrow(new AccessRestrictionException());

        PatronAccessAssignmentRequest request = new PatronAccessAssignmentRequest(agent, pid, accessDetails);
        patronAccessOperationSender.sendUpdateRequest(request);

        verify(patronAccessAssignmentService, timeout(1000)).updatePatronAccess(requestCaptor.capture());
    }
}
