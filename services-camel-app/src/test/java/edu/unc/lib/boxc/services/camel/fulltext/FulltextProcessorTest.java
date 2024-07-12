package edu.unc.lib.boxc.services.camel.fulltext;

import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FulltextProcessorTest {
    private FulltextProcessor processor;
    private final String originalFileName = "full_text.txt";
    private final String testText = "Test text, see if it can be extracted.";
    private final String derivativeFinalPath = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    private File originalFile;
    private File finalDerivativeFile;

    private String derivPath;
    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

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

        derivPath = tmpFolder.toString();
        processor = new FulltextProcessor(derivPath);


        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(RESC_ID);

        finalDerivativeFile = new File(derivPath + "/" + derivativeFinalPath + ".txt");
        // Ensure that final path does not carry over between tests.
        finalDerivativeFile.delete();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void extractFulltextTest() throws Exception {
        originalFile = createFile(originalFileName, testText);

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.toPath().toString());

        processor.process(exchange);
        assertTrue(finalDerivativeFile.exists());
        assertEquals(testText, FileUtils.readFileToString(finalDerivativeFile, UTF_8).trim());
    }

    @Test
    public void extractFromEmptyFileTest() throws Exception {
        originalFile = createFile(originalFileName, "");

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.toPath().toString());

        processor.process(exchange);
        assertTrue(finalDerivativeFile.exists());
        assertEquals("", FileUtils.readFileToString(finalDerivativeFile, UTF_8).trim());
    }

    @Test
    public void extractFromInvalidPdfTest() throws Exception {
        originalFile = createFile("invalid.pdf", "");
        FileUtils.copyFile(new File("src/test/resources/datastreams/invalid.pdf"), originalFile);

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.toPath().toString());

        processor.process(exchange);
        assertFalse(finalDerivativeFile.exists());
    }

    @Test
    public void extractFulltextExceedsCharacterLimit() throws Exception {
        processor.setCharacterLimit(10);

        originalFile = createFile(originalFileName, testText);

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(originalFile.toPath().toString());

        processor.process(exchange);
        assertTrue(finalDerivativeFile.exists());
        assertEquals(testText.substring(0, 10), FileUtils.readFileToString(finalDerivativeFile, UTF_8).trim());
    }

    private File createFile(String name, String content) throws Exception {
        File file = Files.createFile(tmpFolder.resolve(name)).toFile();
        FileUtils.write(file, content, "UTF-8");
        return file;
    }
}
