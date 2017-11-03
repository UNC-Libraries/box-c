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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 *
 * @author harring
 *
 */
public class CollectionObjectTest extends AbstractFedoraTest {

    private PID pid;
    private CollectionObject collection;

    private PID folderChildPid;
    private PID workChildPid;

    @Mock
    private FolderObject folderChildObj;
    @Mock
    private WorkObject workChildObj;
    @Mock
    private CollectionObject collectionChildObj;

    @Before
    public void init() {
        pid = PIDs.get(UUID.randomUUID().toString());

        collection = new CollectionObject(pid, driver, repoObjFactory);

        folderChildPid = pidMinter.mintContentPid();
        when(folderChildObj.getPid()).thenReturn(folderChildPid);
        workChildPid = pidMinter.mintContentPid();
        when(workChildObj.getPid()).thenReturn(workChildPid);

    }

    @Test
    public void isValidTypeTest() {
        // Return the correct RDF types
        List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Collection.getURI());
        when(driver.loadTypes(eq(collection))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                collection.setTypes(types);
                return driver;
            }
        });

        collection.validateType();
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void invalidTypeTest() {
        List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
        when(driver.loadTypes(eq(collection))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                collection.setTypes(types);
                return driver;
            }
        });

        collection.validateType();
    }

    @Test
    public void addWorkMemberTest() {
        collection.addMember(workChildObj);

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(collection), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue("Incorrect type of child added", child instanceof WorkObject);
        assertEquals("Child did not have the expected pid", workChildPid, child.getPid());
    }

    @Test
    public void addFolderMemberTest() {
        collection.addMember(folderChildObj);

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(collection), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue("Incorrect type of child added", child instanceof FolderObject);
        assertEquals("Child did not have the expected pid", folderChildPid, child.getPid());
    }

    // should not be able to add a Collection object as a member
    @Test(expected = ObjectTypeMismatchException.class)
    public void addCollectionObjectMemberTest() {
        collection.addMember(collectionChildObj);
    }

}
