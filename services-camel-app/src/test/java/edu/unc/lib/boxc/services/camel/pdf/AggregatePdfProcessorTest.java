package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.operations.impl.pdf.AggregatePdfService;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSerializationHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AggregatePdfProcessorTest {
    private WorkObject workObject;
    private PID workPid;
    private PID pdfPid;
    private Path finalPdfPath;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    private AggregatePdfProcessor processor;

    private AutoCloseable closeable;

    @Mock
    private AggregatePdfService aggregatePdfService;
    @Mock
    private AccessControlService aclService;
    @Mock
    private PIDMinter pidMinter;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private StorageLocationManager locationManager;
    @Mock
    private StorageLocation storageLocation;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        workPid = TestHelper.makePid();
        pdfPid = TestHelper.makePid();

        workObject = mock(WorkObject.class);
        when(workObject.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getWorkObject(workPid)).thenReturn(workObject);

        finalPdfPath = tmpFolder.resolve("aggregate.pdf");

        var originalFilePid = DatastreamPids.getOriginalFilePid(workPid);

        when(pidMinter.mintContentPid()).thenReturn(pdfPid);
        when(locationManager.getDefaultStorageLocation(workPid)).thenReturn(storageLocation);
        when(storageLocation.getNewStorageUri(originalFilePid)).thenReturn(finalPdfPath.toUri());

        processor = new AggregatePdfProcessor();
        processor.setAclService(aclService);
        processor.setPidMinter(pidMinter);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setLocationManager(locationManager);
        processor.setAggregatePdfService(aggregatePdfService);
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

        verify(aggregatePdfService, never()).generateAggregatePdf(any());
        verify(workObject, never()).addDataFile(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void notWorkTest() throws Exception {
        var anotherPid = TestHelper.makePid();
        var exchange = createRequestExchange(anotherPid.getId(), "image/tiff");

        var anotherOriginalFilePid = DatastreamPids.getOriginalFilePid(anotherPid);
        Path anotherFinalPath = tmpFolder.resolve("another-aggregate.pdf");

        when(locationManager.getDefaultStorageLocation(anotherPid)).thenReturn(storageLocation);
        when(storageLocation.getNewStorageUri(anotherOriginalFilePid)).thenReturn(anotherFinalPath.toUri());

        doThrow(new ObjectTypeMismatchException("not a work object")).when(repositoryObjectLoader)
                .getWorkObject(eq(anotherPid));

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        assertTrue(e.getMessage().contains("Object is not a Work Object"));
        verify(aggregatePdfService, never()).generateAggregatePdf(any());
    }

    @Test
    public void invalidJsonTest() {
        var exchange = TestHelper.mockExchange("{not valid json");

        var e = assertThrows(IllegalArgumentException.class, () -> {
            processor.process(exchange);
        });

        assertTrue(e.getMessage().contains("Failed to deserialize aggregate PDF request"));
        verifyNoInteractions(aggregatePdfService);
    }

    @Test
    public void moveFileFailureTest() throws Exception {
        Path nonexistentAggregatePdf = tmpFolder.resolve("missing.pdf");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(nonexistentAggregatePdf);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        assertThrows(IOException.class, () -> {
            processor.process(exchange);
        });

        verify(workObject, never()).addDataFile(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void existingAggregateFileIsReplacedTest() throws Exception {
        Path firstAggregatePdf = tmpFolder.resolve("first.pdf");
        Files.writeString(firstAggregatePdf, "old content");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(firstAggregatePdf);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");
        processor.process(exchange);

        assertTrue(Files.exists(finalPdfPath));
        assertEquals("old content", Files.readString(finalPdfPath));

        Path secondAggregatePdf = tmpFolder.resolve("second.pdf");
        Files.writeString(secondAggregatePdf, "new content");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(secondAggregatePdf);

        processor.process(exchange);

        assertEquals("new content", Files.readString(finalPdfPath));
    }

    @Test
    public void generateAggregatePdfFailureTest() throws Exception {
        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        when(aggregatePdfService.generateAggregatePdf(any()))
                .thenThrow(new RuntimeException("conversion failed"));

        var e = assertThrows(RuntimeException.class, () -> {
            processor.process(exchange);
        });

        assertEquals(RuntimeException.class, e.getClass());
        assertEquals("conversion failed", e.getMessage());

        assertFalse(Files.exists(finalPdfPath));

        verify(workObject, never()).addDataFile(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void storageLocationRequestedForOriginalFilePidTest() throws Exception {
        Path tmpAggregatePdf = tmpFolder.resolve("aggregate.pdf");
        Files.writeString(tmpAggregatePdf, "pdf content");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(tmpAggregatePdf);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        processor.process(exchange);

        var originalFilePid = DatastreamPids.getOriginalFilePid(workPid);

        verify(locationManager).getDefaultStorageLocation(workPid);
        verify(storageLocation).getNewStorageUri(originalFilePid);
    }

    @Test
    public void addedAggregatePdfIsMarkedAsAggregateFileTest() throws Exception {
        Path tmpAggregatePdf = tmpFolder.resolve("tmp-aggregate.pdf");
        Files.writeString(tmpAggregatePdf, "pdf content");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(tmpAggregatePdf);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        processor.process(exchange);

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);

        verify(workObject).addDataFile(
                eq(pdfPid),
                eq(finalPdfPath.toUri()),
                eq(finalPdfPath.getFileName().toString()),
                eq("application/pdf"),
                isNull(),
                isNull(),
                modelCaptor.capture());

        Model model = modelCaptor.getValue();

        assertNotNull(model);
        assertTrue(model.contains(null, RDF.type, Cdr.AggregateFile));
    }

    @Test
    public void testAggregatePdfProcessor() throws Exception {
        Path tmpAggregatePdf = tmpFolder.resolve("tmp-aggregate.pdf");
        Files.writeString(tmpAggregatePdf, "pdf content");

        when(aggregatePdfService.generateAggregatePdf(any())).thenReturn(tmpAggregatePdf);

        var exchange = createRequestExchange(workPid.getId(), "image/tiff");

        processor.process(exchange);

        verify(aclService).assertHasAccess(
                eq("User does not have permission to generate aggregate PDF"),
                eq(workPid),
                eq(agent.getPrincipals()),
                eq(Permission.runEnhancements));

        verify(repositoryObjectLoader, times(2)).getWorkObject(workPid);
        verify(aggregatePdfService).generateAggregatePdf(any(PdfRequest.class));

        assertTrue(Files.exists(finalPdfPath));
        assertEquals("pdf content", Files.readString(finalPdfPath));
        assertFalse(Files.exists(tmpAggregatePdf));

        verify(workObject).addDataFile(
                eq(pdfPid),
                eq(finalPdfPath.toUri()),
                eq(finalPdfPath.getFileName().toString()),
                eq("application/pdf"),
                isNull(),
                isNull(),
                any(Model.class));
    }

    private Exchange createRequestExchange(String workPid, String mimetype) throws IOException {
        var request = new PdfRequest();
        request.setAgent(agent);
        request.setMimetype(mimetype);
        request.setWorkPid(workPid);

        return TestHelper.mockExchange(PdfRequestSerializationHelper.toJson(request));

    }
}
