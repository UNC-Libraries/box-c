package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdNotificationServiceTest {
    private static final String WORK1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_UUID = "75e29173-2e4d-418d-a9a6-caa68413edaf";
    private static final String EMAIL = "user1@example.com";
    private static final String USERNAME = "user1";
    private AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, new AccessGroupSetImpl("agroup"));
    private BulkRefIdRequest request = new BulkRefIdRequest();
    private BulkRefIdNotificationService service;
    private AutoCloseable closeable;
    @Mock
    private EmailHandler emailHandler;
    @Mock
    private BulkRefIdNotificationBuilder builder;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        request.setAgent(agent);
        service = new BulkRefIdNotificationService();
        service.setBuilder(builder);
        service.setEmailHandler(emailHandler);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendResultsSendsEmail() {
        var emailBody = "Bulk ref ID email";
        when(builder.construct(any(), any(), any())).thenReturn(emailBody);
        request.setEmail(EMAIL);
        var successes = Arrays.asList(PIDs.get(WORK1_UUID), PIDs.get(WORK2_UUID));
        var errors = Arrays.asList("First error", "Another error oh no");
        service.sendResults(request, successes, errors);
        TestHelper.assertEmailSent(emailHandler, EMAIL, emailBody);
    }

    @Test
    public void doNotSendResultsEmailIfNoEmailAddress() {
        var successes = Arrays.asList(PIDs.get(WORK1_UUID), PIDs.get(WORK2_UUID));
        var errors = Arrays.asList("First error", "Another error oh no");
        service.sendResults(request, successes, errors);
        TestHelper.assertEmailNotSent(emailHandler);
    }
}
