package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class ImportThumbnailProcessorTest {
    private ImportThumbnailRequestProcessor processor;

    @TempDir
    private Path path;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private AutoCloseable closeable;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new ImportThumbnailRequestProcessor();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testImportThumbnail() throws IOException {
        var pid = TestHelper.makePid();
        var mimetype = "image/jpeg";
        var request = new ImportThumbnailRequest();
        request.setMimetype(mimetype);
        request.setAgent(agent);
        request.setStoragePath(path);
        request.setPidString(pid.getId());

        var exchange = TestHelper.mockExchange(ImportThumbnailRequestSerializationHelper.toJson(request));
        var message = exchange.getIn();

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, pid.getRepositoryUri());
        verify(message).setHeader(CdrBinaryMimeType, mimetype);
        verify(message).setHeader(CdrBinaryPath, path);
    }
}
