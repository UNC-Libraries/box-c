package edu.unc.lib.boxc.services.camel.images;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.nio.file.Files;
import java.nio.file.Path;

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
    @TempDir
    public Path tmpFolder;

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
    public void testAllowedImageTypeDisallowedType() {
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("text/plain");

        assertFalse(ImageDerivativeProcessor.allowedImageType(exchange));
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

    @Test
    public void cleanupTempFileTest() throws Exception {
        var path = tmpFolder.resolve("tmp_image.tif");
        Files.createFile(path);
        assertTrue(Files.exists(path));
        when(messageIn.getHeader(CdrFcrepoHeaders.CdrImagePathCleanup)).thenReturn(true);
        when(messageIn.getHeader(eq(CdrFcrepoHeaders.CdrImagePath)))
                .thenReturn(path.toString());
        processor.cleanupTempImage(exchange);
        assertFalse(Files.exists(path));
    }

    @Test
    public void cleanupTempFileNoCleanupTest() throws Exception {
        var path = tmpFolder.resolve("tmp_image.tif");
        Files.createFile(path);
        assertTrue(Files.exists(path));
        when(messageIn.getHeader(eq(CdrFcrepoHeaders.CdrImagePath)))
                .thenReturn(path.toString());
        processor.cleanupTempImage(exchange);
        assertTrue(Files.exists(path));

        when(messageIn.getHeader(CdrFcrepoHeaders.CdrImagePathCleanup)).thenReturn(false);
        processor.cleanupTempImage(exchange);
        assertTrue(Files.exists(path));
    }

    @Test
    public void processTest() throws Exception {
        var path = tmpFolder.resolve("tmp_image.tif");

        processor.process(exchange);
        verify(messageIn).setHeader(CdrFcrepoHeaders.CdrImagePath, "path/to/binary");
    }
}
