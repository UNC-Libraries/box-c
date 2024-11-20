package edu.unc.lib.boxc.services.camel.images;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class ImageDerivativeProcessorTest {
    @Mock
    private Exchange exchange;
    @Mock
    private Message messageIn;
    private AutoCloseable closeable;
    private ImageDerivativeProcessor processor;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        when(exchange.getIn()).thenReturn(messageIn);
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryPath)).thenReturn("path/to/binary");

        processor = new ImageDerivativeProcessor();
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testAllowedImageTypePanasonicRaw() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/x-raw-panasonic");

        assertFalse(ImageDerivativeProcessor.allowedImageType(exchange));
    }

    @Test
    public void testAllowedImageTypePanasonicRw2() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/x-panasonic-rw2");

        assertTrue(ImageDerivativeProcessor.allowedImageType(exchange));
    }

    @Test
    public void testAllowedImageTypeIcon() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/x-icon");

        assertFalse(ImageDerivativeProcessor.allowedImageType(exchange));
    }

    @Test
    public void testAllowedImageTypeJpeg() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/jpeg");

        assertTrue(ImageDerivativeProcessor.allowedImageType(exchange));
    }

    @Test
    public void testAllowedImageTypePhotoshop() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("application/photoshop");

        assertTrue(ImageDerivativeProcessor.allowedImageType(exchange));
    }

    @Test
    public void testAllowedImageTypeMicrosoft() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/vnd.microsoft.icon");

        assertFalse(ImageDerivativeProcessor.allowedImageType(exchange));
    }
}
