package edu.unc.lib.boxc.web.services.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.web.services.processing.ImageServerProxyService;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/image-server-proxy-servlet.xml")

@WireMockTest(httpPort = 46887)
public class ImageServerProxyControllerTest extends AbstractAPIIT {
    @Autowired
    private ImageServerProxyService imageServerProxyService;
    @Autowired
    private AccessControlService accessControlService;

    @Mock
    private CloseableHttpClient httpClient;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void getRegionTestNoAccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        when(accessControlService.hasAccess(any(), any(), any())).thenReturn(false);

        mvc.perform(get("/iiif/v3/" + pidString + "/full/max/0/default.jpg"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void getRegionTest() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var formattedBasePath = "/iiif/v3/" + imageServerProxyService.getImageServerEncodedId(pidString) + ".jp2";
        var filename = "bunny.jpg";
        when(accessControlService.hasAccess(any(), any(), any())).thenReturn(true);
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/full/max/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(filename)
                        .withHeader("Content-Type", "image/jpeg")));

        MvcResult result = mvc.perform(get("/iiif/v3/" + pidString + "/full/max/0/default.jpg"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        Assertions.assertEquals(response.getContentAsString(), filename);
    }


    @Test
    void getMetadataTestNoAccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        when(accessControlService.hasAccess(any(), any(), any())).thenReturn(false);

        mvc.perform(get("/iiif/v3/" + pidString + "/info.json"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void getMetadataTest() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var formattedBasePath = "/iiif/v3/" + imageServerProxyService.getImageServerEncodedId(pidString) + ".jp2";
        var json = "{\"@context\":\"http://iiif.io/api/image/3/context.json\",\"id\":\"http://example.com/iiif/v3/"
                + pidString + "\",\"type\":\"ImageService3\",\"protocol\":\"http://iiif.io/api/image\"}";
        when(accessControlService.hasAccess(any(), any(), any())).thenReturn(true);
        stubFor(WireMock.get(urlMatching(formattedBasePath + "/info.json"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        MvcResult result = mvc.perform(get("/iiif/v3/" + pidString + "/info.json"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        Assertions.assertEquals(response.getContentAsString(), json);
    }
}
