package edu.unc.lib.boxc.operations.jms.thumbnail;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author sharonluong
 */
public class ThumbnailRequestSenderTest {
    @Mock
    private JmsTemplate jmsTemplate;
    private ThumbnailRequestSender thumbnailRequestSender;
    private AutoCloseable closeable;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        thumbnailRequestSender = new ThumbnailRequestSender();
        thumbnailRequestSender.setJmsTemplate(jmsTemplate);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendToQueueTest() throws IOException {
        var filePidString = makePid().toString();
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(filePidString);
        request.setAction(ThumbnailRequest.ASSIGN);

        thumbnailRequestSender.sendToQueue(request);
        verify(jmsTemplate).send(any());
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
