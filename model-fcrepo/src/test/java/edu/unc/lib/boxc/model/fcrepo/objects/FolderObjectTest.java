package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            when(driver.loadTypes(eq(folder))).thenAnswer(new Answer<RepositoryObjectDriver>() {
                @Override
                public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                    folder.setTypes(Arrays.asList());
                    return driver;
                }
            });

            folder.validateType();
        });
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
        assertTrue(child instanceof FolderObject, "Incorrect type of child added");
        assertEquals(childPid, child.getPid(), "Child did not have the expected pid");
    }

    @Test
    public void addWorkTest() {
        WorkObjectImpl childObj = new WorkObjectImpl(childPid, driver, repoObjFactory);
        when(repoObjFactory.createWorkObject(isNull())).thenReturn(childObj);

        WorkObject workObj = folder.addWork();

        verify(repoObjFactory).createWorkObject(null);

        assertNotNull(workObj, "Incorrect type of child added");
        assertEquals(childPid, workObj.getPid(), "Child did not have the expected pid");
    }
}
