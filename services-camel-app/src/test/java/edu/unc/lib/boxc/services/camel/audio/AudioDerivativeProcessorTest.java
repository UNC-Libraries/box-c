package edu.unc.lib.boxc.services.camel.audio;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AudioDerivativeProcessorTest {
    @Mock
    private Exchange exchange;
    @Mock
    private Message messageIn;
    private AutoCloseable closeable;
    private AudioDerivativeProcessor processor;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        when(exchange.getIn()).thenReturn(messageIn);
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryPath)).thenReturn("path/to/binary");

        processor = new AudioDerivativeProcessor();
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testAllowedAudioTypeOctetStream() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("application/octet-stream");

        assertFalse(AudioDerivativeProcessor.allowedAudioType(exchange));
    }

    @Test
    public void testAllowedAudioTypeWordperfect() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("application/vnd.wordperfect");

        assertFalse(AudioDerivativeProcessor.allowedAudioType(exchange));
    }

    @Test
    public void testAllowedAudioTypeMpeg() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("audio/mpeg");

        assertTrue(AudioDerivativeProcessor.allowedAudioType(exchange));
    }
}
