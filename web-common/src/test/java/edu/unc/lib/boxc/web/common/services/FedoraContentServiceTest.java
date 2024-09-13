package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.RangeNotSatisfiableException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.apache.jena.rdf.model.ModelFactory;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_RANGE;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

public class FedoraContentServiceTest {
    private AutoCloseable closeable;
    private FedoraContentService fedoraContentService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FileObject fileObject;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private GetBuilder builder;
    @Mock
    private FcrepoResponse fcrepoResponse;
    @Mock
    private ServletOutputStream outputStream;
    @Mock
    private PremisLog premisLog;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        fedoraContentService = new FedoraContentService();
        fedoraContentService.setClient(fcrepoClient);
        fedoraContentService.setAccessControlService(accessControlService);
        fedoraContentService.setRepositoryObjectLoader(repositoryObjectLoader);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(fileObject.getPremisLog()).thenReturn(premisLog);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void streamDataWithBadDatastream() {
        var pid = makePid();

        Assertions.assertThrows(NotFoundException.class, () -> {
            fedoraContentService.streamData(pid, "completely bad", false, response, null);
        });
    }

    @Test
    public void streamDataWithExternalDatastream() {
        var pid = makePid();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            fedoraContentService.streamData(pid, "fulltext", false, response, null);
        });
    }

    @Test
    public void streamDataSuccess() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("image".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("5");

        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, null);

        verify(response).setHeader(CONTENT_LENGTH, "5");
        verify(response).setHeader(CONTENT_DISPOSITION, "inline; filename=\"Best Name\"");
    }

    @Test
    public void streamDataSuccessAsAttachment() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("image".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("5");

        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), true, response, null);

        verify(response).setHeader(CONTENT_LENGTH, "5");
        verify(response).setHeader(CONTENT_DISPOSITION, "attachment; filename=\"Best Name\"");
    }

    @Test
    public void streamDataSuccessOtherDatastream() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        var modsPid = DatastreamPids.getMdDescriptivePid(pid);
        mockWithOriginalFile(pid);
        var modsBinary = mock(BinaryObject.class);
        when(modsBinary.getFilename()).thenReturn("mods.xml");
        when(modsBinary.getPid()).thenReturn(modsPid);
        when(modsBinary.getFilesize()).thenReturn(4L);
        when(repositoryObjectLoader.getBinaryObject(eq(modsPid))).thenReturn(modsBinary);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("desc".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("4");

        fedoraContentService.streamData(pid, DatastreamType.MD_DESCRIPTIVE.getId(), false, response, null);

        verify(response).setHeader(CONTENT_LENGTH, "4");
        verify(response).setHeader(CONTENT_DISPOSITION, "inline; filename=\"mods.xml\"");
    }

    @Test
    public void streamDataWithValidRange() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("imag".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("4");
        var contentRange = "bytes 0-3/5";
        when(fcrepoResponse.getHeaderValue(CONTENT_RANGE)).thenReturn(contentRange);

        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, "bytes=0-3");

        verify(response).setHeader(CONTENT_LENGTH, "4");
        verify(response).setHeader(CONTENT_RANGE, contentRange);
        verify(builder).addHeader("Range", "bytes=0-3");
    }

    @Test
    public void streamDataWithEndRangeSameAsSize() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("image".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("5");
        var contentRange = "bytes 0-4/5";
        when(fcrepoResponse.getHeaderValue(CONTENT_RANGE)).thenReturn(contentRange);

        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, "bytes=0-5");

        verify(response).setHeader(CONTENT_LENGTH, "5");
        verify(response).setHeader(CONTENT_RANGE, contentRange);
        verify(builder).addHeader("Range", "bytes=0-");
    }

    @Test
    public void streamDataWithEndRangeGreaterThanSize() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(fcrepoResponse.getBody()).thenReturn(new ByteArrayInputStream("mage".getBytes(StandardCharsets.UTF_8)));
        when(fcrepoResponse.getHeaderValue(CONTENT_LENGTH)).thenReturn("4");
        var contentRange = "bytes 1-4/5";
        when(fcrepoResponse.getHeaderValue(CONTENT_RANGE)).thenReturn(contentRange);

        fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, "bytes=1-8");

        verify(response).setHeader(CONTENT_LENGTH, "4");
        verify(response).setHeader(CONTENT_RANGE, contentRange);
        verify(builder).addHeader("Range", "bytes=1-");
    }

    @Test
    public void streamDataWithEndRangeInvalid() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        mockWithOriginalFile(pid);
        when(builder.perform()).thenThrow(new FcrepoOperationFailedException(null, 416, "Bad Range"));
        when(fcrepoResponse.getStatusCode()).thenReturn(416);

        assertThrows(RangeNotSatisfiableException.class, () -> {
            fedoraContentService.streamData(pid, ORIGINAL_FILE.getId(), false, response, "bytes=1-bad");
        });
    }

    @Test
    public void streamEventLogTest() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        var model = ModelFactory.createDefaultModel();
        when(premisLog.getEventsModel()).thenReturn(model);
        var logResource = model.getResource(pid.getRepositoryPath());
        logResource.addProperty(DcElements.title, "test title");

        var principals = new AccessGroupSetImpl("group");

        fedoraContentService.streamEventLog(pid, principals, false, response);

        verify(response).setHeader(CONTENT_TYPE, "text/turtle");
        verify(response).setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + pid.getId() + "_event_log.ttl\"");
    }

    @Test
    public void streamEventLogAttachmentTest() throws IOException, FcrepoOperationFailedException {
        var pid = makePid();
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);

        var model = ModelFactory.createDefaultModel();
        when(premisLog.getEventsModel()).thenReturn(model);
        var logResource = model.getResource(pid.getRepositoryPath());
        logResource.addProperty(DcElements.title, "test title");

        var principals = new AccessGroupSetImpl("group");

        fedoraContentService.streamEventLog(pid, principals, true, response);

        verify(response).setHeader(CONTENT_TYPE, "text/turtle");
        verify(response).setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + pid.getId() + "_event_log.ttl\"");
    }

    private void mockWithOriginalFile(PID pid) throws FcrepoOperationFailedException {
        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getOriginalFile()).thenReturn(binaryObject);
        when(binaryObject.getFilename()).thenReturn("Best Name");
        when(binaryObject.getPid()).thenReturn(pid);
        when(binaryObject.getFilesize()).thenReturn(5L);
        when(fcrepoClient.get(any())).thenReturn(builder);
        when(builder.perform()).thenReturn(fcrepoResponse);
    }
}
