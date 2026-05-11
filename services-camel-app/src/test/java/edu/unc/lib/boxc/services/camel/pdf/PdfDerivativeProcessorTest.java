package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.impl.pdf.PdfDerivativeService;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class PdfDerivativeProcessorTest {
    private WorkObject workObject;
    private PID workPid;
    private Path derivativePath;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    private PdfDerivativeProcessor processor;

    private AutoCloseable closeable;

    @Mock
    private PdfDerivativeService pdfDerivativeService;
    @Mock
    private AccessControlService aclService;
    @Mock
    RepositoryObjectLoader repositoryObjectLoader;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        derivativePath = tmpFolder;
        workPid = TestHelper.makePid();
        workObject = mock(WorkObject.class);
        when(workObject.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getWorkObject(workPid)).thenReturn(workObject);

        processor = new PdfDerivativeProcessor(derivativePath.toString());
        processor.setAclService(aclService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setPdfDerivativeService(pdfDerivativeService);
        when(aclService.hasAccess(any(), any(), eq(Permission.runEnhancements))).thenReturn(true);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void insufficientPermissionsTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(any(), any(PID.class), any(), eq(Permission.runEnhancements));

        assertThrows(AccessRestrictionException.class, () -> {
            processor.process(exchange);
        });

        verify(pdfDerivativeService, never()).generatePdfDerivative(any());
    }

    @Test
    public void mimetypeNotAllowedTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), "vnd/x-icon");

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        verify(pdfDerivativeService, never()).generatePdfDerivative(any());
    }

    @Test
    public void mimetypeNotApplicableTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), "text/plain");

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        assertTrue(e.getMessage().contains("not applicable for pdf derivatives"));
        verify(pdfDerivativeService, never()).generatePdfDerivative(any());
    }

    @Test
    public void noMimetypeTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), null);

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        assertTrue(e.getMessage().contains("No mimetype provided"));
        verify(pdfDerivativeService, never()).generatePdfDerivative(any());
    }

    @Test
    public void notWorkTest() throws Exception {
        var anotherPid = TestHelper.makePid();
        var exchange = createRequestExchange(anotherPid.getId(), "image/tiff");

        assertThrows(IllegalArgumentException.class, () -> {
            doThrow(new ObjectTypeMismatchException("not a work object")).when(repositoryObjectLoader)
                    .getWorkObject(eq(anotherPid));
            processor.process(exchange);
        });
    }

    @Test
    public void invalidJsonTest() {
        var exchange = TestHelper.mockExchange("{not valid json");

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        assertTrue(e.getMessage().contains("Failed to deserialize PDF derivative request"));
        verifyNoInteractions(pdfDerivativeService);
    }

    @Test
    public void moveFileFailureTest() throws Exception {
        Path nonexistentDerivative = tmpFolder.resolve("missing.pdf");

        when(pdfDerivativeService.generatePdfDerivative(any())).thenReturn(nonexistentDerivative);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        assertThrows(IOException.class, () -> {
            processor.process(exchange);
        });
    }

    @Test
    public void existingDerivativeIsReplacedTest() throws Exception {
        Path firstDerivative = tmpFolder.resolve("first.pdf");
        Files.writeString(firstDerivative, "old content");

        when(pdfDerivativeService.generatePdfDerivative(any())).thenReturn(firstDerivative);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");
        processor.process(exchange);

        Path finalPath = findGeneratedDerivativePath();
        assertEquals("old content", Files.readString(finalPath));

        Path secondDerivative = tmpFolder.resolve("second.pdf");
        Files.writeString(secondDerivative, "new content");

        when(pdfDerivativeService.generatePdfDerivative(any())).thenReturn(secondDerivative);

        processor.process(exchange);

        assertEquals("new content", Files.readString(finalPath));
    }

    @Test
    public void generatePdfDerivativeFailureTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        when(pdfDerivativeService.generatePdfDerivative(any()))
                .thenThrow(new RuntimeException("conversion failed"));

        var e = assertThrows(RuntimeException.class, () -> {
            processor.process(exchange);
        });

        assertEquals("conversion failed", e.getMessage());

        try (var files = Files.walk(tmpFolder)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().equals(workPid.getId() + ".pdf")));
        }
    }

    @Test
    public void testPdfDerivativeProcessor() throws Exception {
        Path tmpDerivative = tmpFolder.resolve("derivative.pdf");
        Files.writeString(tmpDerivative, "pdf content");

        when(pdfDerivativeService.generatePdfDerivative(any())).thenReturn(tmpDerivative);

        var request = new PdfRequest();
        request.setAgent(agent);
        request.setMimetype("image/tiff");
        request.setWorkPid(workPid.getId());

        String json = PdfRequestSerializationHelper.toJson(request);
        PdfRequest parsed = PdfRequestSerializationHelper.toRequest(json);
        assertNotNull(parsed);

        var exchange = TestHelper.mockExchange(PdfRequestSerializationHelper.toJson(request));

        processor.process(exchange);

        verify(aclService).assertHasAccess(
                eq("User does not have permission to run enhancements"),
                eq(workPid),
                eq(agent.getPrincipals()),
                eq(Permission.runEnhancements));

        verify(repositoryObjectLoader).getWorkObject(workPid);
        verify(pdfDerivativeService).generatePdfDerivative(any(PdfRequest.class));

        Path expectedFinalPath = getExpectedDerivativePath(workPid.getId());

        assertTrue(Files.exists(expectedFinalPath));
        assertEquals("pdf content", Files.readString(expectedFinalPath));
        assertFalse(Files.exists(tmpDerivative));
    }

    private Exchange createRequestExchange(String workPid, String mimetype) throws IOException {
        var request = new PdfRequest();
        request.setAgent(agent);
        request.setMimetype(mimetype);
        request.setWorkPid(workPid);

        return TestHelper.mockExchange(PdfRequestSerializationHelper.toJson(request));

    }

    private Path getExpectedDerivativePath(String binaryId) {
        String derivativePath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        return tmpFolder.resolve(derivativePath).resolve(binaryId + ".pdf");
    }

    private Path findGeneratedDerivativePath() throws IOException {
        try (var files = Files.walk(tmpFolder)) {
            return files
                    .filter(path -> path.getFileName().toString().equals(workPid.getId() + ".pdf"))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
