package edu.unc.lib.boxc.operations.jms.pdf;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class PdfRequestSenderTest {
    private AutoCloseable closeable;
    private final PID workPid = PIDs.get(UUID.randomUUID().toString());
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Mock
    private JmsTemplate jmsTemplate;
    private PdfRequestSender requestSender;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        requestSender = new PdfRequestSender();
        requestSender.setJmsTemplate(jmsTemplate);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendToQueueTest() throws Exception {
        var request = new PdfRequest();
        var agent = new AgentPrincipalsImpl(USERNAME, GROUPS);
        request.setAgent(agent);
        request.setWorkPid(workPid.getId());
        request.setMimetype("image/tiff");

        requestSender.sendToQueue(request);

        verify(jmsTemplate).send(any());
    }
}
