package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static edu.unc.lib.boxc.operations.test.OrderTestHelper.assertHasErrors;
import static edu.unc.lib.boxc.operations.test.OrderTestHelper.mockParentType;

/**
 * @author bbpennel
 */
public class SetOrderValidatorTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private MembershipService membershipService;
    private PID parentPid;
    private PID child1Pid;
    private PID child2Pid;
    private PID child3Pid;
    @Mock
    private RepositoryObject parentObj;

    private SetOrderValidator validator;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        validator = new SetOrderValidator();
        validator.setRepositoryObjectLoader(repositoryObjectLoader);
        validator.setMembershipService(membershipService);

        parentPid = PIDs.get(PARENT_UUID);
        child1Pid = PIDs.get(CHILD1_UUID);
        child2Pid = PIDs.get(CHILD2_UUID);
        child3Pid = PIDs.get(CHILD3_UUID);
        when(repositoryObjectLoader.getRepositoryObject(parentPid)).thenReturn(parentObj);
        mockParentType(parentObj, ResourceType.Work);
    }

    @Test
    public void targetNotAWorkTest() throws Exception {
        mockParentType(parentObj, ResourceType.AdminUnit);
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(
                validator,
                "Object " + PARENT_UUID + " of type AdminUnit does not support member ordering");
    }

    @Test
    public void listedIdsAreNotMembersTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID, CHILD3_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(
                validator,
                "Invalid request to SET order for " + PARENT_UUID
                + ", the following IDs are not members: " + CHILD1_UUID + ", " + CHILD2_UUID);
    }

    @Test
    public void membersNotListedTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child2Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD2_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(
                validator,
                "Invalid request to SET order for " + PARENT_UUID
                + ", the following members were expected but not listed: " + CHILD1_UUID + ", " + CHILD3_UUID);
    }

    @Test
    public void multipleErrorsTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD2_UUID, CHILD3_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(
                validator,
                "Invalid request to SET order for " + PARENT_UUID
                        + ", the following members were expected but not listed: " + CHILD1_UUID,
                "Invalid request to SET order for " + PARENT_UUID
                        + ", the following IDs are not members: " + CHILD2_UUID);
    }

    @Test
    public void duplicateIdsTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child2Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID, CHILD1_UUID, CHILD3_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(validator,"Invalid request to SET order for " + PARENT_UUID
                + ", it contained duplicate member IDs: " + CHILD1_UUID);
    }

    @Test
    public void validRequestTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child2Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID, CHILD3_UUID));
        validator.setRequest(request);

        assertTrue(validator.isValid());
        assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    public void validEmptyRequestTest() throws Exception {
        when(membershipService.listMembers(parentPid)).thenReturn(Collections.emptyList());

        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Collections.emptyList());
        validator.setRequest(request);

        assertTrue(validator.isValid());
        assertTrue(validator.getErrors().isEmpty());
    }
}
