package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.mockito.MockitoAnnotations.openMocks;

@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class EditRefIdIT extends AbstractAPIIT {
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String REF_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @InjectMocks
    private EditRefIdController controller;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
