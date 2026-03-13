package edu.unc.lib.boxc.services.camel.video;

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

public class VideoDerivativeProcessorTest {
    @Mock
    private Exchange exchange;
    @Mock
    private Message messageIn;
    private AutoCloseable closeable;
    private VideoDerivativeProcessor processor;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        when(exchange.getIn()).thenReturn(messageIn);
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryPath)).thenReturn("path/to/binary");

        processor = new VideoDerivativeProcessor();
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testAllowedVideoTypeOctetStream() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("application/octet-stream");

        assertFalse(VideoDerivativeProcessor.allowedVideoType(exchange));
    }

    @Test
    public void testAllowedVideoTypeMp4() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("video/mp4");

        assertTrue(VideoDerivativeProcessor.allowedVideoType(exchange));
    }
}
