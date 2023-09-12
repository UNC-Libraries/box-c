package edu.unc.lib.boxc.services.camel.images;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor.DerivativeGenerationException;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AddDerivativeProcessorTest {

    private final String fileName = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private final String fileExtension = "PNG";
    private String pathId;
    private Path destinationPath;
    private Path generatedDerivPath;

    private AddDerivativeProcessor processor;

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;

    @TempDir
    public Path moveFolder;

    @Mock
    private ExecResult result;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        processor = new AddDerivativeProcessor(fileExtension, moveFolder.toString());

        pathId = PIDs.get(RESC_ID).getId();

        // Derivative file stored with extension
        generatedDerivPath = tmpFolder.resolve(pathId + "." + fileExtension);
        // Path to file from exec result not expected to have extension
        String derivTmpPath = tmpFolder.resolve(pathId).toString();

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn(RESC_ID);

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/png");

        Files.write(generatedDerivPath, Arrays.asList("fake image"));

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(
                derivTmpPath.getBytes()
        ));
        when(message.getBody()).thenReturn(result);

        destinationPath = moveFolder.resolve(fileName + ".PNG");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void createEnhancementTest() throws Exception {
        processor.process(exchange);

        assertTrue(Files.exists(destinationPath));
    }

    @Test
    public void executionFailedTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        String stderr = "convert: no images defined `/tmp/testtxt-large.png'"
                + " @ error/convert.c/ConvertImageCommand/3235.";
        when(result.getStderr()).thenReturn(new ByteArrayInputStream(stderr.getBytes()));

        assertThrows(DerivativeGenerationException.class, () -> {
            processor.process(exchange);
        });
        assertFalse(Files.exists(destinationPath));
    }

    @Test
    public void resultFileIsOnlyAnExtensionTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        when(result.getStdout()).thenReturn(new ByteArrayInputStream(".png".getBytes()));

        assertThrows(DerivativeGenerationException.class, () -> {
            processor.process(exchange);
        });
        assertFalse(Files.exists(destinationPath));
    }

    @Test
    public void resultFileDoesNotExistTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        when(result.getStdout()).thenReturn(new ByteArrayInputStream(tmpFolder.resolve("not_real").toString().getBytes()));

        assertThrows(DerivativeGenerationException.class, () -> {
            processor.process(exchange);
        });
        assertFalse(Files.exists(destinationPath));
    }

    @Test
    public void resultFileDoesNotExistButCommandSucceededTest() throws Exception {
        when(result.getExitValue()).thenReturn(0);
        when(result.getStdout()).thenReturn(new ByteArrayInputStream(tmpFolder.resolve("not_real").toString().getBytes()));

        processor.process(exchange);

        assertFalse(Files.exists(destinationPath));
    }

    @Test
    public void executionExit1TagIgnoredErrorTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        when(result.getStderr()).thenReturn(getClass().getResourceAsStream("/convert/ignore_tag_stderr.txt"));

        processor.process(exchange);

        assertTrue(Files.exists(destinationPath));
    }

    @Test
    public void needsRunNewDerivativeTest() throws Exception {
        assertTrue(processor.needsRun(exchange));
    }

    @Test
    public void needsRunDerivativeAlreadyExistsTest() throws Exception {
        Files.createDirectories(destinationPath.getParent());
        Files.createFile(destinationPath);
        assertFalse(processor.needsRun(exchange));
    }

    @Test
    public void needsRunDerivativeAlreadyExistsForceFlagTest() throws Exception {
        when(message.getHeader(eq("force"))).thenReturn("true");
        Files.createDirectories(destinationPath.getParent());
        Files.createFile(destinationPath);
        assertTrue(processor.needsRun(exchange));
    }

    @Test
    public void cleanupTempFileTest() throws Exception {
        assertTrue(Files.exists(generatedDerivPath));
        when(message.getHeader(eq(CdrFcrepoHeaders.CdrTempPath)))
                .thenReturn(tmpFolder.resolve(pathId).toString());
        processor.cleanupTempFile(exchange);
        assertFalse(Files.exists(generatedDerivPath));
    }
}
