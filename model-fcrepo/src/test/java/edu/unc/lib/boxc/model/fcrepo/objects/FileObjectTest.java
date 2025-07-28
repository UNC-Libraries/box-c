package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.PcdmUse;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FileObjectTest extends AbstractFedoraObjectTest {
    private PID pid;
    private FileObjectImpl fileObject;
    private AutoCloseable closeable;
    private Model model;
    private Resource resc;
    private List<String> types;
    @Captor
    private ArgumentCaptor<Model> modelCaptor;
    @Mock
    private URI storageUri;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        pid = pidMinter.mintContentPid();
        fileObject = new FileObjectImpl(pid, driver, repoObjFactory);
        model = ModelFactory.createDefaultModel();
        resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, PcdmModels.Object);
        resc.addProperty(RDF.type, Cdr.FileObject);

        types = List.of(PcdmModels.Object.getURI(), Cdr.FileObject.getURI());

        when(driver.loadTypes(eq(fileObject))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                fileObject.setTypes(types);
                return driver;
            }
        });

        when(driver.loadModel(eq(fileObject), anyBoolean())).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                fileObject.storeModel(model);
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
        assertEquals(fileObject, fileObject.validateType());
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
            fileObject.validateType();
        });
    }

    @Test
    public void addOriginalFileTest() {
        fileObject.addOriginalFile(storageUri, "file.txt", MediaType.TEXT_PLAIN_VALUE, null, null);
        var originalFilePid = getOriginalFilePid(pid);

        verify(repoObjFactory).createOrUpdateBinary(eq(originalFilePid), eq(storageUri), eq("file.txt"),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), modelCaptor.capture());

        assertTrue(modelCaptor.getValue().getResource(originalFilePid.getRepositoryPath())
                .hasProperty(RDF.type, PcdmUse.OriginalFile));
    }

    @Test
    public void replaceOriginalFileTest() {
        fileObject.replaceOriginalFile(storageUri, "file.txt", MediaType.TEXT_PLAIN_VALUE, null, null);
        var originalFilePid = getOriginalFilePid(pid);

        verify(repoObjFactory).createOrUpdateBinary(eq(originalFilePid), eq(storageUri), eq("file.txt"),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), isNull());
    }

    @Test
    public void getOriginalFileTest() {
        var origFilePid = getOriginalFilePid(pid);
        resc.addProperty(PcdmModels.hasFile, model.createResource(origFilePid.getRepositoryPath()));
        var binObj = mock(BinaryObject.class);
        when(driver.getRepositoryObject(eq(origFilePid), eq(BinaryObject.class))).thenReturn(binObj);

        assertEquals(binObj, fileObject.getOriginalFile());
    }

    @Test
    public void addBinaryTest() {
        var binPid = getTechnicalMetadataPid(pid);
        var binObj = mock(BinaryObject.class);
        when(repoObjFactory.createOrUpdateBinary(eq(binPid), eq(storageUri),eq("file.txt"),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), any(), any())).thenReturn(binObj);

        var createdBinary = fileObject.addBinary(binPid, storageUri, "file.txt",
                MediaType.TEXT_PLAIN_VALUE, null, RDF.type, PcdmUse.Transcript);
        verify(repoObjFactory).createOrUpdateBinary(eq(binPid), eq(storageUri),eq("file.txt"),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), modelCaptor.capture());

        assertTrue(modelCaptor.getValue().getResource(binPid.getRepositoryPath())
                .hasProperty(RDF.type, PcdmUse.Transcript));
        assertEquals(binObj, createdBinary);
    }

    @Test
    public void addBinaryWithAssociationRelationTest() {
        var binPid = getTechnicalMetadataPid(pid);
        var binObj = mock(BinaryObject.class);
        when(repoObjFactory.createOrUpdateBinary(eq(binPid), eq(storageUri), eq("file.txt"),
                eq(MediaType.TEXT_PLAIN_VALUE), any(), isNull(), any())).thenReturn(binObj);

        fileObject.addBinary(binPid, storageUri, "file.txt",
                MediaType.TEXT_PLAIN_VALUE, IanaRelation.derivedfrom, RDF.type, PcdmUse.Transcript);
        verify(repoObjFactory).createRelationship(eq(binObj), eq(IanaRelation.derivedfrom), any());
    }

    @Test
    public void getBinaryObjectsTest() {
        var origFilePid = getOriginalFilePid(pid);
        var technicalMetadataPid = getTechnicalMetadataPid(pid);
        resc.addProperty(PcdmModels.hasFile, model.createResource(origFilePid.getRepositoryPath()));
        resc.addProperty(PcdmModels.hasFile, model.createResource(technicalMetadataPid.getRepositoryPath()));
        var origFileBinObj = mock(BinaryObject.class);
        var technicalMdBinObj = mock(BinaryObject.class);
        when(driver.getRepositoryObject(eq(origFilePid), eq(BinaryObject.class))).thenReturn(origFileBinObj);
        when(driver.getRepositoryObject(eq(technicalMetadataPid), eq(BinaryObject.class))).thenReturn(technicalMdBinObj);

        var binaryObjects = fileObject.getBinaryObjects();
        assertTrue(binaryObjects.contains(origFileBinObj));
        assertTrue(binaryObjects.contains(technicalMdBinObj));
        assertEquals(2, binaryObjects.size());
    }

    @Test
    public void getBinaryObjectTest() {
        var origFilePid = getOriginalFilePid(pid);
        resc.addProperty(PcdmModels.hasFile, model.createResource(origFilePid.getRepositoryPath()));
        var binObj = mock(BinaryObject.class);
        when(driver.getRepositoryObject(eq(origFilePid), eq(BinaryObject.class))).thenReturn(binObj);

        assertEquals(binObj, fileObject.getBinaryObject(ORIGINAL_FILE.getId()));
    }

    @Test
    public void getBinaryObjectNotFoundTest() {
        assertThrows(NotFoundException.class, () -> {
            fileObject.getBinaryObject(ORIGINAL_FILE.getId());
        });
    }

    @Test
    public void getResourceTypeTest() {
        assertEquals(ResourceType.File, fileObject.getResourceType());
    }
}
