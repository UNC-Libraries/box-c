package edu.unc.lib.boxc.services.camel;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.services.camel.NonBinaryEnhancementProcessor;

/**
 * @author lfarrell
 */
public class NonBinaryEnhancementProcessorTest {
    private NonBinaryEnhancementProcessor processor;

    @TempDir
    public Path tmpDir;

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + RESC_ID;

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;
    private File imgFile;
    private String dataDir;

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);
        dataDir = tmpDir.resolve("dataDir").toString();
        Files.createDirectory(tmpDir.resolve("dataDir"));

        TestHelper.setContentBase(FEDORA_BASE);
        processor = new NonBinaryEnhancementProcessor();
        processor.setSourceImagesDir(dataDir);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(FCREPO_URI)).thenReturn(RESC_URI);
    }

    @Test
    public void testCollectionWithImage() throws Exception {
        String derivativeFinalPath = "de/75/d8/11/" + RESC_ID;
        Path uploadedFilePath = Paths.get(dataDir, derivativeFinalPath);
        Files.createDirectories(uploadedFilePath.getParent());

        imgFile = new File(uploadedFilePath.toString());
        FileUtils.writeStringToFile(imgFile, "image file", StandardCharsets.UTF_8);

        processor.process(exchange);

        verify(message).setHeader(CdrBinaryMimeType, "image/*");
        verify(message).setHeader(CdrBinaryPath, imgFile.getAbsolutePath());
    }

    @Test
    public void testCollectionWithoutImage() throws Exception {
        imgFile = new File(dataDir + "/1234");

        processor.process(exchange);

        verify(message, never()).setHeader(CdrBinaryMimeType, "image/*");
        verify(message, never()).setHeader(CdrBinaryPath, imgFile.getAbsolutePath());
    }
}
