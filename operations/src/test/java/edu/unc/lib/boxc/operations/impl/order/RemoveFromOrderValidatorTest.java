package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.test.OrderTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static edu.unc.lib.boxc.operations.test.OrderTestHelper.mockParentType;
import static edu.unc.lib.boxc.operations.test.OrderTestHelper.assertHasErrors;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author snluong
 */
public class RemoveFromOrderValidatorTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private AutoCloseable closeable;
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
    private RemoveFromOrderValidator validator;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        validator = new RemoveFromOrderValidator();
        validator.setRepositoryObjectLoader(repositoryObjectLoader);

        parentPid = PIDs.get(PARENT_UUID);
        child1Pid = PIDs.get(CHILD1_UUID);
        child2Pid = PIDs.get(CHILD2_UUID);
        child3Pid = PIDs.get(CHILD3_UUID);
        when(repositoryObjectLoader.getRepositoryObject(parentPid)).thenReturn(parentObj);
        mockParentType(parentObj, ResourceType.Work);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void targetNotAWorkTest() {
        OrderTestHelper.mockParentType(parentObj, ResourceType.AdminUnit);
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(validator,
                "Object " + PARENT_UUID + " of type AdminUnit does not support member ordering");
    }

    @Test
    public void duplicateIdsTest() {
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child2Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.REMOVE_FROM, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID, CHILD1_UUID));
        validator.setRequest(request);

        assertFalse(validator.isValid());
        assertHasErrors(validator,"Invalid request to REMOVE_FROM order for " + PARENT_UUID
                + ", it contained duplicate member IDs: " + CHILD1_UUID);
    }

    @Test
    public void validRequestTest(){
        when(membershipService.listMembers(parentPid)).thenReturn(Arrays.asList(child1Pid, child2Pid, child3Pid));

        var request = OrderRequestFactory.createRequest(OrderOperationType.REMOVE_FROM, PARENT_UUID,
                List.of(CHILD1_UUID));
        validator.setRequest(request);

        assertTrue(validator.isValid());
        assertTrue(validator.getErrors().isEmpty());
    }
}
