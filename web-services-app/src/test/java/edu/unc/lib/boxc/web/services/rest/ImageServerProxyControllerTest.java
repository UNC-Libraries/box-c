package edu.unc.lib.boxc.web.services.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.web.services.processing.ImageServerProxyService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */
@WireMockTest(httpPort = 46887)
public class ImageServerProxyControllerTest {
    @Mock
    private AccessControlService accessControlService;
    private ImageServerProxyService imageServerProxyService;
    private PoolingHttpClientConnectionManager connectionManager;
    @InjectMocks
    private ImageServerProxyController controller;
    protected MockMvc mvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        connectionManager = new PoolingHttpClientConnectionManager();
        imageServerProxyService = new ImageServerProxyService();
        imageServerProxyService.setImageServerProxyBasePath("http://localhost:46887/iiif/v3/");
        imageServerProxyService.setBaseIiifv3Path("http://example.com/iiif/v3/");
        imageServerProxyService.setHttpClientConnectionManager(connectionManager);
        controller.setImageServerProxyService(imageServerProxyService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        connectionManager.shutdown();
    }

    @Test
    void testGetRegionNoAccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(viewAccessCopies));

        mvc.perform(get("/iiif/v3/" + pidString + "/full/max/0/default.jpg"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void testGetRegionSuccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(pidString);
        var filename = "bunny.jpg";
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/full/max/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(filename)
                        .withHeader("Content-Type", "image/jpeg")));

        MvcResult result = mvc.perform(get("/iiif/v3/" + pidString + "/full/max/0/default.jpg"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        Assertions.assertEquals(filename, response.getContentAsString());
    }

    @Test
    void testGetRegionIOException() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        imageServerProxyService = mock(ImageServerProxyService.class);
        controller.setImageServerProxyService(imageServerProxyService);
        doThrow(new IOException()).when(imageServerProxyService)
                .streamJP2(pidString, "full", "max", "0", "default", "jpg");

        mvc.perform(get("/iiif/v3/" + pidString + "/full/max/0/default.jpg"))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    void testGetMetadataNoAccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(viewAccessCopies));

        mvc.perform(get("/iiif/v3/" + pidString + "/info.json"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void testGetMetadataSuccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(pidString);
        var json = "{\"@context\":\"http://iiif.io/api/image/3/context.json\",\"id\":\"http://example.com/iiif/v3/"
                + pidString + "\",\"type\":\"ImageService3\",\"protocol\":\"http://iiif.io/api/image\"}";
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/info.json"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        MvcResult result = mvc.perform(get("/iiif/v3/" + pidString + "/info.json"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        Assertions.assertEquals(json, response.getContentAsString());
    }

    @Test
    void testGetMetadataIOException() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var formattedBasePath = "/iiif/v3/" + ImageServerUtil.getImageServerEncodedId(pidString);
        imageServerProxyService = mock(ImageServerProxyService.class);
        controller.setImageServerProxyService(imageServerProxyService);
        doThrow(new IOException()).when(imageServerProxyService).getMetadata(pidString);

        stubFor(WireMock.get(urlMatching(formattedBasePath + "/info.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")));

        mvc.perform(get("/iiif/v3/" + pidString + "/info.json"))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }
}
