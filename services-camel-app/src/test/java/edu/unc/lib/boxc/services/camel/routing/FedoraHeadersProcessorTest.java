package edu.unc.lib.boxc.services.camel.routing;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.BASE_URL;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FedoraHeadersProcessorTest {
    private static final String TEST_BASE_URI = "http://example.com/rest";

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    private FedoraHeadersProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new FedoraHeadersProcessor();
        when(exchange.getIn()).thenReturn(message);
    }

    @Test
    public void testProcessReplaceFedoraDescription() throws Exception {
        String baseIdentifier = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441/";
        String originalIdentifier = baseIdentifier + "fedora:description";
        String expectedIdentifier = baseIdentifier + RepositoryPathConstants.FCR_METADATA;
        String fcrepoUri = TEST_BASE_URI + expectedIdentifier;

        when(message.getHeader(FCREPO_URI, String.class)).thenReturn(null);
        when(message.getHeader(IDENTIFIER, String.class)).thenReturn(originalIdentifier);
        when(message.getHeader(BASE_URL, String.class)).thenReturn(TEST_BASE_URI);

        processor.process(exchange);

        verify(message).setHeader(IDENTIFIER, expectedIdentifier);
        verify(message).setHeader(FCREPO_URI, fcrepoUri);
    }

    @Test
    public void testProcessIdentifierWithoutFedoraDescription() throws Exception {
        String identifier = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441/original_file";
        String fcrepoUri = TEST_BASE_URI + identifier;

        when(message.getHeader(FCREPO_URI, String.class)).thenReturn(null);
        when(message.getHeader(IDENTIFIER, String.class)).thenReturn(identifier);
        when(message.getHeader(BASE_URL, String.class)).thenReturn(TEST_BASE_URI);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, fcrepoUri);
    }

    @Test
    public void testProcessNullIdentifier() throws Exception {
        when(message.getHeader(FCREPO_URI, String.class)).thenReturn(null);
        when(message.getHeader(IDENTIFIER, String.class)).thenReturn(null);
        when(message.getHeader(BASE_URL, String.class)).thenReturn(TEST_BASE_URI);

        processor.process(exchange);

        verify(message, never()).setHeader(eq(FCREPO_URI), anyString());
    }

    @Test
    public void testProcessNullFcrepoUri() throws Exception {
        String identifier = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441/original_file";

        when(message.getHeader(FCREPO_URI, String.class)).thenReturn(null);
        when(message.getHeader(IDENTIFIER, String.class)).thenReturn(identifier);
        when(message.getHeader(BASE_URL, String.class)).thenReturn(null);

        processor.process(exchange);

        verify(message, never()).setHeader(eq(FCREPO_URI), anyString());
    }
}