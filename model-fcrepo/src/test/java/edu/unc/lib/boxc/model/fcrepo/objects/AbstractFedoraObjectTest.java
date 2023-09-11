package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 *
 * @author harring
 *
 */
public class AbstractFedoraObjectTest {

    protected static final String FEDORA_BASE = "http://example.com/";

    private AutoCloseable closeable;

    @Mock
    protected RepositoryObjectDriver driver;
    @Mock
    protected RepositoryPaths repoPaths;
    @Mock
    protected RepositoryObjectFactory repoObjFactory;

    protected PIDMinter pidMinter;

    @BeforeEach
    public void initBase() {
        closeable = openMocks(this);

        pidMinter = new RepositoryPIDMinter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
