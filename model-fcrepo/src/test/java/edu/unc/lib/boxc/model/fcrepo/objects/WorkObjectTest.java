package edu.unc.lib.boxc.model.fcrepo.objects;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidRelationshipException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 *
 * @author bbpennel
 *
 */
public class WorkObjectTest extends AbstractFedoraObjectTest {

    private static final String FILENAME = "file.txt";
    private static final String MIMETYPE = "plain/txt";
    private static final String SHA1 = "sha";
    private static final String MD5 = "md5";

    private PID pid;

    private WorkObjectImpl work;

    private AutoCloseable closeable;

    @Mock
    private FileObject fileObj;
    private URI contentUri;

    private List<String> types;
    private Model model;
    private Resource resc;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        pid = makePid();

        model = ModelFactory.createDefaultModel();
        resc = model.getResource(pid.getURI());
        resc.addProperty(RDF.type, PcdmModels.Object);
        resc.addProperty(RDF.type, Cdr.Work);

        work = new WorkObjectImpl(pid, driver, repoObjFactory);

        when(fileObj.getParent()).thenReturn(work);
        contentUri = URI.create("file:///path/to/file");

        types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Work.getURI());
        when(driver.loadTypes(eq(work))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                work.setTypes(types);
                return driver;
            }
        });

        when(driver.loadModel(eq(work), anyBoolean())).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                work.storeModel(model);
                return driver;
            }
        });
        when(driver.getRepositoryObject(any(PID.class))).thenReturn(fileObj);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void validTypeTest() {
        assertEquals(work, work.validateType());
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());

            assertEquals(work, work.validateType());
        });
    }

    @Test
    public void setPrimaryObjectTest() {
        PID primaryPid = makePid();
        Resource primaryResc = createResource(primaryPid.getURI());

        when(fileObj.getResource()).thenReturn(primaryResc);

        work.setPrimaryObject(primaryPid);

        verify(repoObjFactory).createExclusiveRelationship(any(RepositoryObject.class), eq(Cdr.primaryObject),
                eq(primaryResc));
    }

    @Test
    public void setPrimaryObjectToNonMemberTest() {
        Assertions.assertThrows(InvalidRelationshipException.class, () -> {
            PID anotherPid =  makePid();
            WorkObject anotherWork = mock(WorkObject.class);
            when(anotherWork.getPid()).thenReturn(anotherPid);

            // Assign the file object to a different parent
            when(fileObj.getParent()).thenReturn(anotherWork);

            PID primaryPid = makePid();

            try {
                work.setPrimaryObject(primaryPid);
            } finally {
                verify(repoObjFactory, never()).createExclusiveRelationship(any(RepositoryObject.class),
                        eq(Cdr.primaryObject), any(Resource.class));
            }
        });
    }

    @Test
    public void setPrimaryObjectToInvalidTypeTest() {
        Assertions.assertThrows(InvalidOperationForObjectType.class, () -> {
            PID anotherPid =  makePid();
            WorkObject anotherWork = mock(WorkObject.class);
            when(anotherWork.getPid()).thenReturn(anotherPid);

            when(driver.getRepositoryObject(eq(anotherPid))).thenReturn(anotherWork);

            try {
                work.setPrimaryObject(anotherPid);
            } finally {
                verify(repoObjFactory, never()).createExclusiveRelationship(any(RepositoryObject.class),
                        eq(Cdr.primaryObject), any(Resource.class));
            }
        });
    }

    @Test
    public void clearPrimaryObjectTest() {
        work.clearPrimaryObject();

        verify(repoObjFactory).deleteProperty(eq(work), eq(Cdr.primaryObject));
    }

    @Test
    public void getPrimaryObjectTest() {
        PID primaryPid = makePid();
        Resource primaryResc = createResource(primaryPid.getRepositoryPath());

        when(driver.getRepositoryObject(eq(primaryPid), eq(FileObject.class))).thenReturn(fileObj);

        resc.addProperty(Cdr.primaryObject, primaryResc);

        FileObject resultObj = work.getPrimaryObject();

        assertEquals(resultObj, fileObj);
    }

    @Test
    public void getNoPrimaryObjectTest() {
        FileObject resultObj = work.getPrimaryObject();

        assertNull(resultObj);
        verify(driver, never()).getRepositoryObject(any(PID.class), eq(FileObject.class));
    }

    @Test
    public void addMemberFileObjectTest() {

        work.addMember(fileObj);

        verify(repoObjFactory).addMember(eq(work), eq(fileObj));
    }

    @Test
    public void addMemberFolderTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            FolderObject folderObj = mock(FolderObject.class);

            work.addMember(folderObj);

            verify(repoObjFactory).addMember(any(ContentObject.class), any(ContentObject.class));
        });
    }

    @Test
    public void addDataFileTest() {
        when(repoObjFactory.createFileObject(any(Model.class))).thenReturn(fileObj);

        // Add the data file
        work.addDataFile(contentUri, FILENAME, MIMETYPE, SHA1, MD5);

        ArgumentCaptor.forClass(PID.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(repoObjFactory).createFileObject(modelCaptor.capture());

        Model fileObjModel = modelCaptor.getValue();

        assertTrue(fileObjModel.contains(null, DC.title, FILENAME));

        verify(fileObj).addOriginalFile(contentUri, FILENAME, MIMETYPE, SHA1, MD5);
        verify(repoObjFactory).addMember(eq(work), eq(fileObj));
    }

    @Test
    public void addDataFileWithPropertiesTest() {
        // Construct model with extra properties to add to the data file
        Model extraProperties = ModelFactory.createDefaultModel();
        Resource dataResc = extraProperties.getResource("");
        dataResc.addProperty(CdrAcl.none, PUBLIC_PRINC);

        when(repoObjFactory.createFileObject(any(Model.class))).thenReturn(fileObj);

        // Add the data file with properties
        FileObject fileObj = work.addDataFile(contentUri, FILENAME, MIMETYPE, SHA1, MD5, extraProperties);

        ArgumentCaptor.forClass(PID.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);

        verify(repoObjFactory).createFileObject(modelCaptor.capture());

        Model fileObjModel = modelCaptor.getValue();

        assertTrue(fileObjModel.contains(null, DC.title, FILENAME));
        assertTrue(fileObjModel.contains(null, CdrAcl.none, PUBLIC_PRINC));

        verify(fileObj).addOriginalFile(contentUri, FILENAME, MIMETYPE, SHA1, MD5);
        verify(repoObjFactory).addMember(eq(work), eq(fileObj));
    }

    @Test
    public void addDataFileNoContentTest() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> work.addDataFile(null, FILENAME, MIMETYPE, SHA1, MD5));
    }

    @Test
    public void getMemberOrderNotSet() {
        assertEquals(Collections.emptyList(), work.getMemberOrder());
    }

    @Test
    public void getMemberOrderWithMembers() {
        var member1 = makePid();
        var member2 = makePid();
        var member3 = makePid();
        var members = Arrays.asList(member1, member2, member3);
        var memberValue = member1.getId() + "|" + member2.getId() + "|" + member3.getId();
        resc.addProperty(Cdr.memberOrder, memberValue);
        assertEquals(members, work.getMemberOrder());
    }

    @Test
    public void getThumbnailObjectTest() {
        PID pid = makePid();
        Resource resource = createResource(pid.getRepositoryPath());

        when(driver.getRepositoryObject(eq(pid), eq(FileObject.class))).thenReturn(fileObj);

        resc.addProperty(Cdr.useAsThumbnail, resource);

        FileObject resultObj = work.getThumbnailObject();

        assertEquals(resultObj, fileObj);
    }

    @Test
    public void getNoThumbnailObjectTest() {
        FileObject resultObj = work.getThumbnailObject();

        assertNull(resultObj);
        verify(driver, never()).getRepositoryObject(any(PID.class), eq(FileObject.class));
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
