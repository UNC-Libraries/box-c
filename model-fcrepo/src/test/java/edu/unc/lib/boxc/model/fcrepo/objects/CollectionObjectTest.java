package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 *
 * @author harring
 *
 */
public class CollectionObjectTest extends AbstractFedoraObjectTest {

    private PID pid;
    private CollectionObjectImpl collection;

    private PID folderChildPid;
    private PID workChildPid;

    @Mock
    private FolderObject folderChildObj;
    @Mock
    private WorkObject workChildObj;
    @Mock
    private CollectionObject collectionChildObj;

    @BeforeEach
    public void init() {
        pid = PIDs.get(UUID.randomUUID().toString());

        collection = new CollectionObjectImpl(pid, driver, repoObjFactory);

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

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
            when(driver.loadTypes(eq(collection))).thenAnswer(new Answer<RepositoryObjectDriver>() {
                @Override
                public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                    collection.setTypes(types);
                    return driver;
                }
            });

            collection.validateType();
        });
    }

    @Test
    public void addWorkMemberTest() {
        collection.addMember(workChildObj);

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(collection), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue(child instanceof WorkObject, "Incorrect type of child added");
        assertEquals(workChildPid, child.getPid(), "Child did not have the expected pid");
    }

    @Test
    public void addFolderMemberTest() {
        collection.addMember(folderChildObj);

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(collection), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue(child instanceof FolderObject, "Incorrect type of child added");
        assertEquals(folderChildPid, child.getPid(), "Child did not have the expected pid");
    }

    // should not be able to add a Collection object as a member
    @Test
    public void addCollectionObjectMemberTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> collection.addMember(collectionChildObj));

    }

}
