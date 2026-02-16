package edu.unc.lib.boxc.services.camel.video;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;

import mp44u.CLIMain;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class Mp44uVideoProcessorTest {
    private final String fileName = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private final String mp44uThreads = "8";
    private String derivTmpPath;

    private Mp44uVideoProcessor processor;

    private AutoCloseable closeable;

    @Mock
    private Exchange exchange;
    @Mock
    private ExecResult result;
    @Mock
    private Message message;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        processor = new Mp44uVideoProcessor(mp44uThreads, 60);

        // Path to file from exec result not expected to have extension
        derivTmpPath = tmpFolder.resolve(fileName).toString();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(CdrFcrepoHeaders.CdrTempPath)).thenReturn(derivTmpPath);
        when(message.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("video/mp4");

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(
                derivTmpPath.getBytes()
        ));
        when(message.getBody()).thenReturn(result);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testMp44uAudioProcessor() throws Exception {
        when(message.getHeader(CdrFcrepoHeaders.CdrVideoPath)).thenReturn(fileName);
        try (MockedStatic<CLIMain> mockedStatic = mockStatic(CLIMain.class)) {
            mockedStatic.when(() -> CLIMain.runCommand(new String[]{anyString()}))
                    .thenAnswer((Answer<Void>) invocation -> null);
            processor.process(exchange);

            mockedStatic.verify(() -> CLIMain.runCommand(new String[]{"mp44u", "video",
                    "-i", "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc",
                    "-o",  derivTmpPath,
                    "-t", "8",
                    "-T", "60"}));
        }
    }

    @Test
    public void testNoFileInput() throws Exception {
        try (MockedStatic<CLIMain> mockedStatic = mockStatic(CLIMain.class)) {
            mockedStatic.when(() -> CLIMain.runCommand(new String[]{anyString()}))
                    .thenThrow(IllegalArgumentException.class);
            processor.process(exchange);

            mockedStatic.verify(() -> CLIMain.runCommand(new String[]{"mp44u", "video",
                    "-i", null,
                    "-o", derivTmpPath,
                    "-t", "8",
                    "-T", "60"}));
        }
    }
}
