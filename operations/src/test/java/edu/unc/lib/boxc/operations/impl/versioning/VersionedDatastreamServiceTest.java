package edu.unc.lib.boxc.operations.impl.versioning;

import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.exceptions.StateUnmodifiedException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class VersionedDatastreamServiceTest {
    private final static String TEST_SHA1 = "60cff04894583ce46f68d80a376eea30f5473f11";
    private VersionedDatastreamService service;

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private BinaryTransferService transferService;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private FedoraTransaction mockTx;
    @Mock
    private BinaryObject dsObj;
    @Mock
    private BinaryObject dsHistoryObj;
    @Mock
    private FileObject fileObj;
    @Mock
    private BinaryTransferSession session;
    @Mock
    private BinaryTransferOutcome dsOutcome;
    private URI destinationUri;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new VersionedDatastreamService();
        service.setBinaryTransferService(transferService);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setTransactionManager(transactionManager);
        when(transactionManager.startTransaction()).thenReturn(mockTx);
        when(transferService.getSession(any(RepositoryObject.class))).thenReturn(session);
        destinationUri = URI.create("file://path/to/my/file.xml");
        when(session.transferReplaceExisting(any(), any(InputStream.class))).thenReturn(dsOutcome);
        when(dsOutcome.getDestinationUri()).thenReturn(destinationUri);
        when(dsOutcome.getSha1()).thenReturn(TEST_SHA1);
        when(session.transferReplaceExisting(any(), any(InputStream.class))).thenReturn(dsOutcome);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void addVersion_DatastreamWithSameContent() throws Exception {
        PID pid = TestHelper.makePid();
        PID dsPid = DatastreamPids.getDatastreamPid(pid, DatastreamType.MD_DESCRIPTIVE);
        PID dsHistoryPid = DatastreamPids.getDatastreamHistoryPid(dsPid);

        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(fileObj);
        when(repoObjLoader.getBinaryObject(dsPid)).thenReturn(dsObj);
        setupDatastreamObject(dsPid);

        VersionedDatastreamService.DatastreamVersion newV2 = new VersionedDatastreamService.DatastreamVersion(dsPid);
        newV2.setContentStream(getModsDocumentStream());
        newV2.setContentType("text/xml");

        service.addVersion(newV2);

        verify(session).transferReplaceExisting(eq(dsPid), any(InputStream.class));
        verify(repoObjFactory).createOrUpdateBinary(eq(dsHistoryPid), any(), isNull(), eq("text/xml"),
                any(), isNull(), isNull());
        verify(session).transferReplaceExisting(eq(dsHistoryPid), any(InputStream.class));
    }

    @Test
    public void addVersion_DatastreamWithSameContentSkipUnchanged() throws Exception {
        PID pid = TestHelper.makePid();
        PID dsPid = DatastreamPids.getDatastreamPid(pid, DatastreamType.MD_DESCRIPTIVE);
        PID dsHistoryPid = DatastreamPids.getDatastreamHistoryPid(dsPid);

        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(fileObj);
        when(repoObjLoader.getBinaryObject(dsPid)).thenReturn(dsObj);
        setupDatastreamObject(dsPid);

        VersionedDatastreamService.DatastreamVersion newV2 = new VersionedDatastreamService.DatastreamVersion(dsPid);
        newV2.setContentStream(getModsDocumentStream());
        newV2.setContentType("text/xml");
        newV2.setSkipUnmodified(true);

        assertThrows(StateUnmodifiedException.class, () -> {
            service.addVersion(newV2);
        });

        verify(session, never()).transferReplaceExisting(eq(dsPid), any(InputStream.class));
        verify(repoObjFactory, never()).createOrUpdateBinary(eq(dsHistoryPid), any(), isNull(), eq("text/xml"),
                any(), isNull(), isNull());
        verify(session, never()).transferReplaceExisting(eq(dsHistoryPid), any(InputStream.class));
    }

    private void setupDatastreamObject(PID dsPid) throws IOException {
        when(dsObj.getPid()).thenReturn(dsPid);
        when(dsObj.getMimetype()).thenReturn("text/xml");
        when(dsObj.getLastModified()).thenReturn(new Date());
        when(dsObj.getCreatedDate()).thenReturn(new Date());
        when(dsObj.getSha1Checksum()).thenReturn("urn:sha1:" + TEST_SHA1);
        when(dsObj.getBinaryStream()).thenReturn(getModsDocumentStream());
    }

    private InputStream getModsDocumentStream() throws IOException {
        return Files.newInputStream(Paths.get("src/test/resources/samples/mods.xml"));
    }
}
