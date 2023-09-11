package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class OrderValidatorFactoryTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private AutoCloseable closeable;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private MembershipService membershipService;
    private OrderValidatorFactory factory;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        factory = new OrderValidatorFactory();
        factory.setMembershipService(membershipService);
        factory.setRepositoryObjectLoader(repositoryObjectLoader);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void forSetOrderRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID, Arrays.asList(CHILD1_UUID));
        assertTrue(factory.createValidator(request) instanceof SetOrderValidator);
    }

    @Test
    public void forClearOrderRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.CLEAR, PARENT_UUID);
        assertTrue(factory.createValidator(request) instanceof ClearOrderValidator);
    }

    @Test
    public void forRemoveFromOrderRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.REMOVE_FROM, PARENT_UUID, Arrays.asList(CHILD1_UUID));
        assertTrue(factory.createValidator(request) instanceof RemoveFromOrderValidator);
    }
}
