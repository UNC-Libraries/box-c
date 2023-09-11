package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class OrderJobFactoryTest {
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String PARENT_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private AutoCloseable closeable;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    private OrderJobFactory factory;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        factory = new OrderJobFactory();
        factory.setRepositoryObjectFactory(repositoryObjectFactory);
        factory.setRepositoryObjectLoader(repositoryObjectLoader);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void createClearRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.CLEAR, PARENT_UUID);
        var job = factory.createJob(request);
        assertTrue(job instanceof ClearOrderJob);
    }

    @Test
    public void createSetRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID));
        var job = factory.createJob(request);
        assertTrue(job instanceof SetOrderJob);
    }

    @Test
    public void createUnsupportedOperationequestTest() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            var request = OrderRequestFactory.createRequest(OrderOperationType.ADD_TO, PARENT_UUID,
                    Arrays.asList(CHILD1_UUID));
            factory.createJob(request);
        });
    }
}
