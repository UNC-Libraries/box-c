package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.test.OrderTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class SetOrderJobTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private AutoCloseable closeable;

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
        closeable = openMocks(this);
        parentPid = PIDs.get(PARENT_UUID);
        orderJobFactory = new OrderJobFactory();
        orderJobFactory.setRepositoryObjectFactory(repositoryObjectFactory);
        orderJobFactory.setRepositoryObjectLoader(repositoryObjectLoader);
        when(repositoryObjectLoader.getRepositoryObject(parentPid)).thenReturn(parentWork);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void singleChildTest() {
        var request = OrderTestHelper.createRequest(OrderOperationType.SET, PARENT_UUID, CHILD1_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();

        assertMemberOrderSetWithValue(CHILD1_UUID);
    }

    @Test
    public void multipleChildrenTest() {
        var request = OrderTestHelper.createRequest(OrderOperationType.SET, PARENT_UUID, CHILD1_UUID, CHILD2_UUID, CHILD3_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();

        assertMemberOrderSetWithValue(CHILD1_UUID + "|" + CHILD2_UUID + "|" + CHILD3_UUID);
    }

    @Test
    public void multipleChildrenAlternateOrderTest() {
        var request = OrderTestHelper.createRequest(OrderOperationType.SET, PARENT_UUID, CHILD2_UUID, CHILD3_UUID, CHILD1_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();

        assertMemberOrderSetWithValue(CHILD2_UUID + "|" + CHILD3_UUID + "|" + CHILD1_UUID);
    }

    private void assertMemberOrderSetWithValue(String expectedValue) {
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(parentWork), eq(Cdr.memberOrder), childrenValueCaptor.capture());
        String calledWith = (String) childrenValueCaptor.getValue();
        assertEquals(expectedValue, calledWith);
    }
}
