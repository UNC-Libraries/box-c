package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getDepositManifestPid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DepositRecordTest extends AbstractFedoraObjectTest {
    private AutoCloseable closeable;
    private PID pid;
    private DepositRecordImpl depositRecord;
    private Resource resc;
    private Model model;
    private List<String> types;
    @Mock
    private URI manifestUri;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        pid = pidMinter.mintDepositRecordPid();
        depositRecord = new DepositRecordImpl(pid, driver, repoObjFactory);
        types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.DepositRecord.getURI());
        model = ModelFactory.createDefaultModel();
        resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, PcdmModels.Object);
        resc.addProperty(RDF.type, Cdr.DepositRecord);

        when(driver.loadTypes(eq(depositRecord))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                depositRecord.setTypes(types);
                return driver;
            }
        });

        when(driver.loadModel(eq(depositRecord), anyBoolean())).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                depositRecord.storeModel(model);
                return driver;
            }
        });
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void isValidTypeTest() {
        assertEquals(depositRecord, depositRecord.validateType());
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
            depositRecord.validateType();
        });
    }

    @Test
    public void addManifestTest() {
        var filename = "file.txt";
        depositRecord.addManifest(manifestUri, filename, MediaType.TEXT_PLAIN_VALUE, null, null);
        var manifestPid = getDepositManifestPid(pid, filename);
        verify(repoObjFactory).createOrUpdateBinary(eq(manifestPid), eq(manifestUri), eq(filename),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), isNull());
    }

    @Test
    public void addManifestNoFilenameTest() {
        manifestUri = URI.create("manifest/string.txt");
        var filename = "string.txt";
        depositRecord.addManifest(manifestUri, null, MediaType.TEXT_PLAIN_VALUE, null, null);
        var manifestPid = getDepositManifestPid(pid, filename);
        verify(repoObjFactory).createOrUpdateBinary(eq(manifestPid), eq(manifestUri), eq(filename),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), isNull());
    }

    @Test
    public void addManifestNoMimetypeTest() {
        var filename = "file.txt";
        depositRecord.addManifest(manifestUri, filename, null, null, null);
        var manifestPid = getDepositManifestPid(pid, filename);
        verify(repoObjFactory).createOrUpdateBinary(eq(manifestPid), eq(manifestUri), eq(filename),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), isNull());
    }

    @Test
    public void getManifestTest() {
        var binObjPid = getDepositManifestPid(pid, "file.txt");
        var binObj = mock(BinaryObjectImpl.class);
        when(driver.getRepositoryObject(eq(binObjPid), eq(BinaryObjectImpl.class))).thenReturn(binObj);
        assertEquals(binObj, depositRecord.getManifest(binObjPid));
    }

    @Test
    public void getManifestNoComponentTest() {
        var randomPid = pidMinter.mintContentPid();
        assertNull(depositRecord.getManifest(randomPid));
    }

    @Test
    public void listManifestsTest() {

    }

    @Test
    public void listDepositedObjectsTest() {

    }
}