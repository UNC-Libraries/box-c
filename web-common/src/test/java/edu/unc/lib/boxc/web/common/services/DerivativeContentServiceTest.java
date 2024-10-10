package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DerivativeContentServiceTest {
    private AutoCloseable closeable;
    private DerivativeContentService derivativeContentService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private DerivativeService derivativeService;

    @BeforeEach
    public void init()  {
        closeable = openMocks(this);
        derivativeContentService = new DerivativeContentService();
        derivativeContentService.setAccessControlService(accessControlService);
        derivativeContentService.setDerivativeService(derivativeService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
