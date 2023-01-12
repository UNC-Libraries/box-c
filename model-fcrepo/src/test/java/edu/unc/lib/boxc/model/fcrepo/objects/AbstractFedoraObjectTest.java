package edu.unc.lib.boxc.model.fcrepo.objects;

import static org.mockito.MockitoAnnotations.initMocks;

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

    @Mock
    protected RepositoryObjectDriver driver;
    @Mock
    protected RepositoryPaths repoPaths;
    @Mock
    protected RepositoryObjectFactory repoObjFactory;

    protected PIDMinter pidMinter;

    @BeforeEach
    public void initBase() {
        initMocks(this);

        pidMinter = new RepositoryPIDMinter();
    }

}
