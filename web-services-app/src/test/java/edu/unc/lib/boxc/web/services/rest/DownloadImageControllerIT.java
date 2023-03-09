
package edu.unc.lib.boxc.web.services.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * @author snluong
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/download-image-it-servlet.xml")

@WireMockTest(httpPort = 46887)
public class DownloadImageControllerIT extends AbstractAPIIT {
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private SolrQueryLayerService solrSearchService;

    private String iiifBasePath = "http://localhost:46887/";

    @Test
    public void testGetImageAtFullSize() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        Datastream datastream = mock(Datastream.class);

        stubFor(WireMock.get(urlMatching("/" + filePid + "/full/full/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile("src/test/resources/upload-files/bunny.jpg")
                        .withHeader("Content-Type", "image/jpeg")));

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        when(record.getDatastreamObject(filePid.getId())).thenReturn(datastream);
        when(datastream.getExtent()).thenReturn("800x1200");

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();

        assertEquals("image/jpeg", response.getContentType());
        assertEquals("attachment; filename=\"image_full.jpg\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testFullSizeAccessImageNoPermissions() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewOriginal));

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("Insufficient permissions", response.getContentAsString());
    }
}
