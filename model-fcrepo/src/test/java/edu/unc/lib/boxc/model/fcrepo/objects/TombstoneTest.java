package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TombstoneTest extends AbstractFedoraObjectTest {
    private PID pid;
    private AutoCloseable closeable;
    private TombstoneImpl tombstone;
    private List<String> types;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        pid = pidMinter.mintContentPid();
        tombstone = new TombstoneImpl(pid, driver, repoObjFactory);
        types = List.of(PcdmModels.Object.getURI(), Fcrepo4Repository.Tombstone.getURI());

        when(driver.loadTypes(eq(tombstone))).thenAnswer(new Answer<RepositoryObjectDriver>() {
            @Override
            public RepositoryObjectDriver answer(InvocationOnMock invocation) throws Throwable {
                tombstone.setTypes(types);
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
        assertEquals(tombstone, tombstone.validateType());
    }

    @Test
    public void invalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
            tombstone.validateType();
        });
    }

    @Test
    public void getParentTest() {
        assertNull(tombstone.getParent());
    }

    @Test
    public void getParentPidTest() {
        assertNull(tombstone.getParentPid());
    }
}
