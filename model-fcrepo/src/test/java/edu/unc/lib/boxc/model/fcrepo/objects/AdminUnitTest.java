package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * @author harring
 */
public class AdminUnitTest extends AbstractFedoraObjectTest {

    private PID pid;
    private AdminUnit unit;

    private PID collectionChildPid;

    private AutoCloseable closeable;

    @Mock
    private CollectionObject collectionChildObj;
    @Mock
    private WorkObject workChildObj;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());

        unit = new AdminUnitImpl(pid, driver, repoObjFactory);

        collectionChildPid = PIDs.get(UUID.randomUUID().toString());
        when(collectionChildObj.getPid()).thenReturn(collectionChildPid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void isValidTypeTest() {
        // Return the correct RDF types
        List<String> types = Arrays.asList(PcdmModels.Collection.getURI(), Cdr.AdminUnit.getURI());
        when(driver.loadTypes(eq(unit))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                ((AdminUnitImpl) unit).setTypes(types);
                return driver;
            }
        });

        unit.validateType();
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Work.getURI());
            when(driver.loadTypes(eq(unit))).thenAnswer(new Answer<RepositoryObjectDriver>() {
                @Override
                public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                    ((AdminUnitImpl) unit).setTypes(types);
                    return driver;
                }
            });

            unit.validateType();
        });
    }

    // should not be able to add a Work object as a member of AdminUnit
    @Test
    public void addWorkMemberTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> unit.addMember(workChildObj));
    }

    @Test
    public void addCollectionObjectMemberTest() {
        unit.addMember(collectionChildObj);
        pidMinter.mintContentPid();

        ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
        verify(repoObjFactory).addMember(eq(unit), captor.capture());

        ContentObject child = captor.getValue();
        assertTrue(child instanceof CollectionObject, "Incorrect type of child added");
        assertEquals(collectionChildPid, child.getPid(), "Child did not have the expected pid");
    }

}
