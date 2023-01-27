package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.test.OrderTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static edu.unc.lib.boxc.operations.test.OrderTestHelper.createRequest;

/**
 * @author snluong
 */
public class RemoveFromJobTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";

    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private WorkObject parentWork;
    private OrderJobFactory orderJobFactory;
    private PID parentPid;
    @Captor
    private ArgumentCaptor<Object> childrenValueCaptor;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        parentPid = PIDs.get(PARENT_UUID);
        orderJobFactory = new OrderJobFactory();
        orderJobFactory.setRepositoryObjectFactory(repositoryObjectFactory);
        orderJobFactory.setRepositoryObjectLoader(repositoryObjectLoader);
        when(repositoryObjectLoader.getWorkObject(parentPid)).thenReturn(parentWork);
        var listOfPids = Arrays.asList(PIDs.get(CHILD1_UUID), PIDs.get(CHILD2_UUID), PIDs.get(CHILD3_UUID));
        // need to make list modifiable
        var childrenOrder = new LinkedList<>(listOfPids);
        when(parentWork.getMemberOrder()).thenReturn(childrenOrder);
    }

    @Test
    public void removeOneChildFromMemberOrder() {
        var request = createRequest(OrderOperationType.REMOVE_FROM, PARENT_UUID, CHILD1_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();
        assertMemberOrderSetWithValue(CHILD2_UUID + "|" + CHILD3_UUID);
    }

    @Test
    public void removeChildrenFromMemberOrder() {
        var request = createRequest(OrderOperationType.REMOVE_FROM, PARENT_UUID, CHILD1_UUID, CHILD2_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();

        assertMemberOrderSetWithValue(CHILD3_UUID);
    }

    private void assertMemberOrderSetWithValue(String expectedValue) {
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(parentWork), eq(Cdr.memberOrder), childrenValueCaptor.capture());
        String calledWith = (String) childrenValueCaptor.getValue();
        assertEquals(expectedValue, calledWith);
    }
}
