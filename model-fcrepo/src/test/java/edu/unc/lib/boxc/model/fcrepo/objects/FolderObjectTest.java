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
package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class FolderObjectTest extends AbstractFedoraObjectTest {

    private PID pid;

    private PID childPid;

    private FolderObjectImpl folder;

    @Before
    public void init() {

        pid = pidMinter.mintContentPid();

        folder = new FolderObjectImpl(pid, driver, repoObjFactory);

        childPid = pidMinter.mintContentPid();
    }

    @Test
    public void isValidTypeTest() {
        // Return the correct RDF types
        List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
        when(driver.loadTypes(eq(folder))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                folder.setTypes(types);
                return driver;
            }
        });

        folder.validateType();
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void invalidTypeTest() {
        when(driver.loadTypes(eq(folder))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                folder.setTypes(Arrays.asList());
                return driver;
            }
        });

        folder.validateType();
    }

    @Test
    public void addFolderTest() {
        FolderObjectImpl childFolder = new FolderObjectImpl(childPid, driver, repoObjFactory);

        when(repoObjFactory.createFolderObject(isNull()))
                .thenReturn(childFolder);

        folder.addFolder();

        verify(repoObjFactory).createFolderObject((Model) isNull());

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(folder), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue("Incorrect type of child added", child instanceof FolderObject);
        assertEquals("Child did not have the expected pid", childPid, child.getPid());
    }

    @Test
    public void addWorkTest() {
        WorkObjectImpl childObj = new WorkObjectImpl(childPid, driver, repoObjFactory);
        when(repoObjFactory.createWorkObject(isNull())).thenReturn(childObj);

        WorkObject workObj = folder.addWork();

        verify(repoObjFactory).createWorkObject(null);

        assertTrue("Incorrect type of child added", workObj != null);
        assertEquals("Child did not have the expected pid", childPid, workObj.getPid());
    }
}
