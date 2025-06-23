package edu.unc.lib.boxc.services.camel.images;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePathCleanup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author bbpennel
 */
public class PdfImageProcessorTest {

    private PdfImageProcessor processor;
    private Exchange exchange;
    private Message message;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        processor = new PdfImageProcessor();
        processor.setOutputPath(tempDir.toString());

        exchange = new DefaultExchange(new DefaultCamelContext());
        message = exchange.getIn();
    }

    @Test
    public void testProcessPdf() throws Exception {
        // Get the path to the test PDF
        String pdfPath = Paths.get("src", "test", "resources", "boxy.pdf").toAbsolutePath().toString();

        // Setup the exchange with the PDF path
        message.setHeader(CdrBinaryPath, pdfPath);
        message.setHeader(CdrBinaryMimeType, "application/pdf");

        // Process the PDF
        processor.process(exchange);

        // Verify the headers are set correctly
        String outputPath = message.getHeader(CdrImagePath, String.class);
        assertTrue(outputPath.contains(".tif"), "Output path should contain .tif extension");
        assertEquals("image/tiff", message.getHeader(CdrBinaryMimeType, String.class),
                "Mime type should be set to image/tiff");
        assertTrue(message.getHeader(CdrImagePathCleanup, Boolean.class),
                "Temp path cleanup flag should be set to true");

        // Verify the output file exists
        var outputFile = Paths.get(outputPath);
        assertTrue(Files.exists(outputFile), "Generated TIFF file should exist");
        assertTrue(Files.size(outputFile) > 0, "Generated TIFF file should not be empty");
    }

    @Test
    public void testLoadThrowsIOException() throws Exception {
        // Set a non-existent PDF path
        message.setHeader(CdrBinaryPath, "nonexistent.pdf");
        message.setHeader(CdrBinaryMimeType, "application/pdf");

        // An IOException is thrown internally, but we are demonstrating that it does not bubble up
        processor.process(exchange);

        // Since it failed to generate an image from the PDF no image path should be set, so camel will skip further processing
        assertNull(message.getHeader(CdrImagePath), "CdrImagePath should not be set when exception occurs");
        assertNull(message.getHeader(CdrImagePathCleanup));
        assertEquals("application/pdf", message.getHeader(CdrBinaryMimeType));
    }

    @Test
    public void testIsPdfFileTrue() {
        message.setHeader(CdrBinaryMimeType, "application/pdf");
        assertTrue(PdfImageProcessor.isPdfFile(exchange), "Should identify PDF file correctly");
    }

    @Test
    public void testIsPdfFileFalse() {
        message.setHeader(CdrBinaryMimeType, "image/jpeg");
        assertFalse(PdfImageProcessor.isPdfFile(exchange), "Should not identify non-PDF as PDF");
    }
}
