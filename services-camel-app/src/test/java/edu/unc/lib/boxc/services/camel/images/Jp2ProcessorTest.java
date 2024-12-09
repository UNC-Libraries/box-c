package edu.unc.lib.boxc.services.camel.images;

import JP2ImageConverter.CLIMain;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
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

public class Jp2ProcessorTest {
    private final String fileName = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private String derivTmpPath;

    private Jp2Processor processor;

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

        processor = new Jp2Processor();

        // Path to file from exec result not expected to have extension
        derivTmpPath = tmpFolder.resolve(fileName).toString();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(CdrFcrepoHeaders.CdrTempPath)).thenReturn(derivTmpPath);
        when(message.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType)).thenReturn("image/tiff");

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
    public void testJp2Processor() throws Exception {
        when(message.getHeader(CdrFcrepoHeaders.CdrImagePath)).thenReturn(fileName);
        try (MockedStatic<CLIMain> mockedStatic = mockStatic(CLIMain.class)) {
            mockedStatic.when(() -> CLIMain.main(new String[]{anyString()})).thenAnswer((Answer<Void>) invocation -> null);
            processor.process(exchange);

            mockedStatic.verify(() -> CLIMain.main(new String[]{"jp24u", "kdu_compress",
                    "-f", "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc", "-o",  derivTmpPath,
                    "-sf", "image/tiff"}));
        }
    }

    @Test
    public void testNoFileInput() throws Exception {
        try (MockedStatic<CLIMain> mockedStatic = mockStatic(CLIMain.class)) {
            mockedStatic.when(() -> CLIMain.main(new String[]{anyString()})).thenThrow(IllegalArgumentException.class);
            processor.process(exchange);

            mockedStatic.verify(() -> CLIMain.main(new String[]{"jp24u", "kdu_compress", "-f", null,
                    "-o", derivTmpPath, "-sf", "image/tiff"}));
        }
    }
}
