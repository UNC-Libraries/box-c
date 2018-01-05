/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fcrepo4;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 *
 * @author bbpennel
 *
 */
public class WorkObjectTest extends AbstractFedoraTest {

    private static final String FILENAME = "file.txt";
    private static final String MIMETYPE = "plain/txt";
    private static final String SHA1 = "sha";
    private static final String MD5 = "md5";

    private PID pid;

    private WorkObject work;

    @Mock
    private FileObject fileObj;
    @Mock
    private InputStream contentStream;

    private List<String> types;
    private Model model;
    private Resource resc;

    @Before
    public void init() {
        initMocks(this);

        pid = makePid();

        model = ModelFactory.createDefaultModel();
        resc = model.getResource(pid.getURI());
        resc.addProperty(RDF.type, PcdmModels.Object);
        resc.addProperty(RDF.type, Cdr.Work);

        work = new WorkObject(pid, driver, repoObjFactory);

        types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Work.getURI());
        when(driver.loadTypes(eq(work))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                work.setTypes(types);
                return driver;
            }
        });

        when(driver.loadModel(eq(work))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                work.storeModel(model);
                return driver;
            }
        });
        when(driver.getRepositoryObject(any(PID.class))).thenReturn(fileObj);
    }

    @Test
    public void validTypeTest() {
        assertEquals(work, work.validateType());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void invalidTypeTest() {
        types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());

        assertEquals(work, work.validateType());
    }

    @Test
    public void setPrimaryObjectTest() {
        PID primaryPid = makePid();
        Resource primaryResc = createResource(primaryPid.getURI());

        resc.addProperty(PcdmModels.hasMember, primaryResc);

        work.setPrimaryObject(primaryPid);

        verify(repoObjFactory).createExclusiveRelationship(any(RepositoryObject.class), eq(Cdr.primaryObject),
                eq(primaryResc));
    }

    @Test(expected = InvalidRelationshipException.class)
    public void setPrimaryObjectToNonMemberTest() {
        PID primaryPid = makePid();

        try {
            work.setPrimaryObject(primaryPid);
        } finally {
            verify(repoObjFactory, never()).createExclusiveRelationship(any(RepositoryObject.class),
                    eq(Cdr.primaryObject), any(Resource.class));
        }
    }

    @Test
    public void getPrimaryObjectTest() {
        PID primaryPid = makePid();
        Resource primaryResc = createResource(primaryPid.getURI());

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

    @Test(expected = ObjectTypeMismatchException.class)
    public void addMemberFolderTest() {
        FolderObject folderObj = mock(FolderObject.class);

        work.addMember(folderObj);

        verify(repoObjFactory).addMember(any(ContentObject.class), any(ContentObject.class));
    }

    @Test
    public void addDataFileTest() {
        when(repoObjFactory.createFileObject(any(Model.class))).thenReturn(fileObj);

        // Add the data file
        work.addDataFile(contentStream, FILENAME, MIMETYPE, SHA1, MD5);

        ArgumentCaptor.forClass(PID.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(repoObjFactory).createFileObject(modelCaptor.capture());

        Model fileObjModel = modelCaptor.getValue();

        assertTrue(fileObjModel.contains(null, DC.title, FILENAME));

        verify(fileObj).addOriginalFile(contentStream, FILENAME, MIMETYPE, SHA1, MD5);
        verify(repoObjFactory).addMember(eq(work), eq(fileObj));
    }

    @Test
    public void addDataFileWithPropertiesTest() {
        // Construct model with extra properties to add to the data file
        Model extraProperties = ModelFactory.createDefaultModel();
        Resource dataResc = extraProperties.getResource("");
        dataResc.addProperty(CdrAcl.patronAccess, PatronAccess.none.name());

        when(repoObjFactory.createFileObject(any(Model.class))).thenReturn(fileObj);

        // Add the data file with properties
        FileObject fileObj = work.addDataFile(contentStream, FILENAME, MIMETYPE, SHA1, MD5, extraProperties);

        ArgumentCaptor.forClass(PID.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);

        verify(repoObjFactory).createFileObject(modelCaptor.capture());

        Model fileObjModel = modelCaptor.getValue();

        assertTrue(fileObjModel.contains(null, DC.title, FILENAME));
        assertTrue(fileObjModel.contains(null, CdrAcl.patronAccess, PatronAccess.none.name()));

        verify(fileObj).addOriginalFile(contentStream, FILENAME, MIMETYPE, SHA1, MD5);
        verify(repoObjFactory).addMember(eq(work), eq(fileObj));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addDataFileNoContentTest() {

        work.addDataFile(null, FILENAME, MIMETYPE, SHA1, MD5);
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
