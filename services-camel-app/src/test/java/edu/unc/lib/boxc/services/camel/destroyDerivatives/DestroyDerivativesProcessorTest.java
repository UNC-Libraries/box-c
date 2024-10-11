package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.model.api.DatastreamType.FULLTEXT_EXTRACTION;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

public class DestroyDerivativesProcessorTest {
    private DestroyDerivativesProcessor processor;
    private File file;
    private String pathId;
    private String derivativeDirBase;
    private File derivativeTypeBaseDir;
    private File derivativeFinalDir;
    private String derivativeTypeDir;
    private static final String FEDORA_BASE = "http://example.com/rest/";
    private static final String PID_UUID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String PID_ID = "content/" + PID_UUID;
    private static final String PID_PATH = "content/de/75/d8/11/" + PID_UUID;
    private static final String RESC_ID = FEDORA_BASE + PID_PATH;
    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        pathId = PIDs.get(RESC_ID).getId();

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(CdrBinaryPidId)))
                .thenReturn(PID_ID);

        derivativeDirBase = tmpFolder.toString();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void deleteFulltextTest() throws Exception {
        derivativeTypeDir = FULLTEXT_EXTRACTION.getId();
        derivativeFinalDir = Files.createDirectories(tmpFolder.resolve(derivativeTypeDir + "/de/75/d8/11")).toFile();
        file = new File(derivativeFinalDir, PID_UUID + ".txt");

        FileUtils.writeStringToFile(file, "my text", StandardCharsets.UTF_8);

        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor("txt", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("text/plain");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted parent dirs
        assertFalse(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }

    @Test
    public void deleteJp2Test() throws Exception {
        derivativeTypeDir = JP2_ACCESS_COPY.getId();
        derivativeFinalDir = Files.createDirectories(tmpFolder.resolve(derivativeTypeDir + "/de/75/d8/11")).toFile();
        file = new File(derivativeFinalDir, pathId + ".jp2");

        FileUtils.writeStringToFile(file, "fake jp2", StandardCharsets.UTF_8);

        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor("jp2", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/jp2");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted parent dirs
        assertFalse(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }

    @Test
    public void multipleDirectoryTest() throws Exception {
        derivativeTypeDir = JP2_ACCESS_COPY.getId();

        // Derivative to destroy
        derivativeFinalDir = Files.createDirectories(tmpFolder.resolve(derivativeTypeDir + "/de/75/d8/11")).toFile();
        file = new File(derivativeFinalDir, pathId + ".jp2");
        FileUtils.writeStringToFile(file, "fake jp2", StandardCharsets.UTF_8);

        // Another derivative at the same level
        File siblingDir = Files.createDirectories(tmpFolder.resolve(derivativeTypeDir + "/de/55/c8/21")).toFile();
        String siblingPidId = "de55c821-9e0f-4b1f-8631-2060ab3580cc";
        String siblingRescId = FEDORA_BASE + "content/de/55/c8/21/" + siblingPidId;
        String siblingPathId = PIDs.get(siblingRescId).getId();
        File siblingFile = new File(siblingDir, siblingPathId + ".jp2");
        FileUtils.writeStringToFile(siblingFile, "fake jp2 too", StandardCharsets.UTF_8);

        // Run processor
        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor("jp2", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/jp2");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted highest unique derivative dir
        assertFalse(new File(derivativeTypeBaseDir, "75").exists());
        // Didn't delete common parent dir
        assertTrue(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete sibling derivative
        assertTrue(siblingFile.exists());
    }

    @Test
    public void multipleFilesInDirectoryTest() throws Exception {
        derivativeTypeDir = JP2_ACCESS_COPY.getId();
        derivativeFinalDir = Files.createDirectories(tmpFolder.resolve(derivativeTypeDir + "/de/75/d8/11")).toFile();

        // Derivative to destroy
        file = new File(derivativeFinalDir, pathId + ".jp2");
        FileUtils.writeStringToFile(file, "fake jp2", StandardCharsets.UTF_8);

        // Another file at the same level
        File siblingFile = new File(derivativeFinalDir, pathId + ".png");
        FileUtils.writeStringToFile(siblingFile, "fake png", StandardCharsets.UTF_8);

        // Run processor
        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor("jp2", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/jp2");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Didn't delete sibling
        assertTrue(siblingFile.exists());
        // Didn't delete parent directory
        assertTrue(derivativeFinalDir.exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }
}
