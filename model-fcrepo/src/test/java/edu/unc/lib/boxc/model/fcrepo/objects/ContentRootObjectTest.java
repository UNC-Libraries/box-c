package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ContentRootObjectTest extends AbstractFedoraObjectTest {
    private PID pid;
    private AutoCloseable closeable;
    private ContentRootObjectImpl contentRootObject;
    private List<String> types;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        pid = pidMinter.mintContentPid();
        contentRootObject = new ContentRootObjectImpl(pid, driver, repoObjFactory);
        types = List.of(PcdmModels.Object.getURI(), Cdr.ContentRoot.getURI());

        when(driver.loadTypes(eq(contentRootObject))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                contentRootObject.setTypes(types);
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
        assertEquals(contentRootObject,contentRootObject.validateType());
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
            contentRootObject.validateType();
        });
    }

    @Test
    public void getResourceTypeTest() {
        assertEquals(ResourceType.ContentRoot, contentRootObject.getResourceType());
    }

    @Test
    public void addMemberAdminUnitTest() {
        var adminUnit = mock(AdminUnit.class);
        contentRootObject.addMember(adminUnit);

        verify(repoObjFactory).addMember(eq(contentRootObject), eq(adminUnit));
    }

    @Test
    public void addMemberNonAdminUnitTest() {
        var fileObject = mock(FileObject.class);
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            contentRootObject.addMember(fileObject);
        });
    }
}
