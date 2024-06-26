package edu.unc.lib.boxc.services.camel.patronAccess;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewReducedQuality;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.apache.camel.BeanInject;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.UUID;

/**
 * @author bbpennel
 */
public class PatronAccessAssignmentRouterTest extends CamelSpringTestSupport {
    private static final String USER = "someone";
    private static final String PRINCIPALS = "my:special:group;everyone;authenticated";
    private AutoCloseable closeable;

    @BeanInject(value = "patronAccessAssignmentService")
    private PatronAccessAssignmentService patronAccessAssignmentService;

    @BeanInject(value = "patronAccessOperationSender")
    private PatronAccessOperationSender patronAccessOperationSender;

    @Captor
    private ArgumentCaptor<PatronAccessAssignmentRequest> requestCaptor;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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
    public void assignReducedQualityRoleTest() throws Exception {
        AgentPrincipals agent = new AgentPrincipalsImpl(USER, new AccessGroupSetImpl(PRINCIPALS));
        PID pid = PIDs.get(UUID.randomUUID().toString());
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(PUBLIC_PRINC, canViewReducedQuality),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals)));

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
