package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ImportThumbnailProcessorTest {
    private ImportThumbnailRequestProcessor processor;

    @TempDir
    private Path path;
    private Path tempThumbnailPath;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private Exchange exchange;
    private Message message;
    private String mimetype = "image/jpeg";
    private PID pid;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new ImportThumbnailRequestProcessor();
        pid = TestHelper.makePid();
        tempThumbnailPath = path.resolve(pid.getId() + ".png");

        var request = new ImportThumbnailRequest();
        request.setMimetype(mimetype);
        request.setAgent(agent);
        request.setStoragePath(path);
        request.setPidString(pid.getId());

        exchange = TestHelper.mockExchange(ImportThumbnailRequestSerializationHelper.toJson(request));
        message = exchange.getIn();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testImportThumbnail() throws IOException {
        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, pid.getRepositoryPath());
        verify(message).setHeader(CdrBinaryMimeType, mimetype);
        verify(message).setHeader(CdrBinaryPath, path.toString());
    }

    @Test
    public void cleanupTempThumbnailFileTest() throws Exception {
        Files.write(tempThumbnailPath, List.of("fake image"));
        assertTrue(Files.exists(tempThumbnailPath));
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(tempThumbnailPath.toString());
        processor.cleanupTempThumbnailFile(exchange);
        assertFalse(Files.exists(tempThumbnailPath));
    }
}
