package edu.unc.lib.boxc.services.camel.streaming;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.DURACLOUD;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.OPEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class StreamingPropertiesProcessorTest {
    private StreamingPropertiesRequestProcessor processor;
    private FileObject fileObject;
    private PID filePid;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new StreamingPropertiesRequestProcessor();
        processor.setAclService(accessControlService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
        filePid = ProcessorTestHelper.makePid();
        fileObject = mock(FileObject.class);
        when(fileObject.getPid()).thenReturn(filePid);
        when(repositoryObjectLoader.getFileObject(filePid)).thenReturn(fileObject);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testStreamingPropertiesUpdateNoPermission() throws IOException {
        var exchange = createRequestExchange(OPEN, "banjo_recording.mp3", filePid.getId());

        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(any(), any(PID.class), any(), eq(Permission.ingest));
            processor.process(exchange);
        });
    }

    @Test
    public void testStreamingPropertiesUpdateNoFilename() throws IOException {
        var exchange = createRequestExchange(OPEN, null, filePid.getId());
        processor.process(exchange);

    }

    @Test
    public void testStreamingPropertiesUpdateNoFolder() throws IOException {
        var exchange = createRequestExchange(null, "banjo_recording.mp3", filePid.getId());
        processor.process(exchange);

    }

    @Test
    public void testStreamingPropertiesUpdateNotAFileObject() throws IOException {
        var anotherPid = ProcessorTestHelper.makePid();
        var exchange = createRequestExchange(OPEN, "banjo_recording.mp3", anotherPid.getId());
        processor.process(exchange);

    }

    @Test
    public void testStreamingPropertiesUpdateSuccess() throws IOException {
        var exchange = createRequestExchange(OPEN, "banjo_recording.mp3", filePid.getId());
        processor.process(exchange);

        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.streamingFile), eq("banjo_recording-playlist.m3u8"));
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.streamingFolder), eq(OPEN));
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.streamingHost), eq(DURACLOUD));
    }

    private Exchange createRequestExchange(String folder, String filename, String pid) throws IOException {
        var request = new StreamingPropertiesRequest();
        request.setAgent(agent);
        request.setFilename(filename);
        request.setFilePidString(pid);
        request.setFolder(folder);
        return ProcessorTestHelper.mockExchange(StreamingPropertiesRequestSerializationHelper.toJson(request));
    }

}
