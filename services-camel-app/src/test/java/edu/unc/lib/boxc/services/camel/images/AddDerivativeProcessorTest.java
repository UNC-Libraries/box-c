package edu.unc.lib.boxc.services.camel.images;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;

public class AddDerivativeProcessorTest {

    private final String fileName = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private final String fileExtension = "PNG";
    private String pathId;
    private File file;
    private File mvFile;

    private AddDerivativeProcessor processor;

    private File finalDir;

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    @TempDir
    public Path tmpDir;

    @TempDir
    public Path moveDir;

    @Mock
    private ExecResult result;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        finalDir = moveDir.toFile();

        processor = new AddDerivativeProcessor(fileExtension, finalDir.getAbsolutePath());

        pathId = PIDs.get(RESC_ID).getId();

        // Derivative file stored with extension
        file = tmpDir.resolve(pathId + "." + fileExtension).toFile();
        String derivTmpPath = file.getAbsolutePath();
        // Path to file from exec result not expected to have extension
        derivTmpPath = derivTmpPath.substring(0, derivTmpPath.length() - fileExtension.length() - 1);

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn(RESC_ID);

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/png");

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write("fake image");
        }

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(
                derivTmpPath.getBytes()
        ));
        when(message.getBody()).thenReturn(result);
    }

    @Test
    public void createEnhancementTest() throws Exception {
        mvFile = new File(finalDir.getAbsolutePath() + "/"+ fileName + ".PNG");
        processor.process(exchange);

        assertTrue(mvFile.exists());
    }

    @Test
    public void executionFailedTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        String stderr = "convert: no images defined `/tmp/testtxt-large.png'"
                + " @ error/convert.c/ConvertImageCommand/3235.";
        when(result.getStderr()).thenReturn(new ByteArrayInputStream(stderr.getBytes()));

        mvFile = new File(finalDir.getAbsolutePath() + "/"+ fileName + ".PNG");
        processor.process(exchange);

        assertFalse(mvFile.exists());
    }

    @Test
    public void resultFileDoesNotExistTest() throws Exception {
        Assertions.assertThrows(IOException.class, () -> {
            when(result.getStdout()).thenReturn(new ByteArrayInputStream(".png".getBytes()));

            processor.process(exchange);
        });
    }

    @Test
    public void executionExit1TagIgnoredErrorTest() throws Exception {
        when(result.getExitValue()).thenReturn(1);
        when(result.getStderr()).thenReturn(getClass().getResourceAsStream("/convert/ignore_tag_stderr.txt"));

        mvFile = new File(finalDir.getAbsolutePath() + "/"+ fileName + ".PNG");
        processor.process(exchange);

        assertTrue(mvFile.exists());
    }
}
