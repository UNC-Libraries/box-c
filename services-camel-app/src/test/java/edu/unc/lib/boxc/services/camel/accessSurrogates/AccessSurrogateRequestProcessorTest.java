package edu.unc.lib.boxc.services.camel.accessSurrogates;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.DELETE;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.SET;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AccessSurrogateRequestProcessorTest {
    private AccessSurrogateRequestProcessor processor;
    private PID filePid;
    private Path path;
    private Path accessSurrogatePath;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private DerivativeService derivativeService;
    @Mock
    private MessageSender messageSender;
    @TempDir
    public Path requestFolder;
    @TempDir
    public Path derivativeFolder;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new AccessSurrogateRequestProcessor();
        processor.setAccessControlService(accessControlService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setDerivativeService(derivativeService);
        processor.setMessageSender(messageSender);
        filePid = ProcessorTestHelper.makePid();
        FileObject fileObject = mock(FileObject.class);
        path = requestFolder.resolve(filePid.getId());
        accessSurrogatePath = derivativeFolder.resolve(filePid.getId());
        when(fileObject.getPid()).thenReturn(filePid);
        when(repositoryObjectLoader.getRepositoryObject(filePid)).thenReturn(fileObject);
        when(derivativeService.getDerivativePath(eq(filePid), eq(DatastreamType.ACCESS_SURROGATE)))
                .thenReturn(accessSurrogatePath);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateAccessSurrogateNoPermission() throws JsonProcessingException {
        var exchange = setRequestExchange();

        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(any(), any(PID.class), any(), eq(Permission.editDescription));
            processor.process(exchange);
        });
    }

    @Test
    public void testUpdateAccessSurrogateNotAFile() throws Exception {
        var workPid = ProcessorTestHelper.makePid();
        var workObject = mock(WorkObject.class);
        when(repositoryObjectLoader.getRepositoryObject(eq(workPid))).thenReturn(workObject);
        var workTempPath = requestFolder.resolve(workPid.getId());
        var exchange = createRequestExchange(workPid.getId(), workTempPath, SET);
        var workAccessSurrogatePath = derivativeFolder.resolve(workPid.getId());
        when(derivativeService.getDerivativePath(eq(workPid), eq(DatastreamType.ACCESS_SURROGATE)))
                .thenReturn(workAccessSurrogatePath);

        processor.process(exchange);
        assertFalse(Files.exists(workAccessSurrogatePath));
        assertFalse(Files.exists(workTempPath));
    }

    @Test
    public void testSetAccessSurrogate() throws Exception {
        var exchange = setRequestExchange();
        Files.write(path, List.of("fake image"));

        processor.process(exchange);
        assertTrue(Files.exists(accessSurrogatePath));
        assertFalse(Files.exists(path));
        verify(exchange.getIn()).setHeader(FCREPO_URI, filePid.getRepositoryPath());
    }

    @Test
    public void testDeleteAccessSurrogate() throws Exception {
        var exchange = createRequestExchange(filePid.getId(), null, DELETE);
        Files.write(accessSurrogatePath, List.of("fake image"));

        processor.process(exchange);
        assertFalse(Files.exists(accessSurrogatePath));
        assertFalse(Files.exists(path));
        verify(exchange.getIn()).setHeader(FCREPO_URI, filePid.getRepositoryPath());
    }

    private Exchange createRequestExchange(String pidString, Path path, String action) throws JsonProcessingException {
        var request = new AccessSurrogateRequest();
        request.setAction(action);
        request.setAgent(agent);
        request.setMimetype("image/jpeg");
        request.setFilePath(path);
        request.setPidString(pidString);
        return ProcessorTestHelper.mockExchange(AccessSurrogateRequestSerializationHelper.toJson(request));
    }

    private Exchange setRequestExchange() throws JsonProcessingException {
        return createRequestExchange(filePid.getId(), path, SET);
    }
}
