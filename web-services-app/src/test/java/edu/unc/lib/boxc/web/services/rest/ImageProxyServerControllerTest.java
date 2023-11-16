package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.web.services.processing.ImageServerProxyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author snluong
 */
public class ImageProxyServerControllerTest {
    private static final String IMAGE_SERVER_PROXY_PATH = "http://cantaloupe.com/iiif/v3/";
    private static final String BASE_IIIF_V3_PATH = "http://example.com/iiif/v3/";
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private AutoCloseable closeable;
    private ImageServerProxyService imageServerProxyService;

    @InjectMocks
    private ImageServerProxyController imageServerProxyController;
    @Mock
    private AccessControlService accessControlService;


    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        imageServerProxyService = new ImageServerProxyService();
        imageServerProxyService.setImageServerProxyBasePath(IMAGE_SERVER_PROXY_PATH);
        imageServerProxyService.setBaseIiifv3Path(BASE_IIIF_V3_PATH);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
