
package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewMetadata;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
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
import org.apache.commons.io.FileUtils;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import java.io.File;
import java.io.IOException;

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

    @Test
    public void testGetImageAtFullSize() throws Exception {
        var pidString = makePid().getId();
        var formattedPid = idToPath(pidString, 4, 2) + pidString + ".jp2";

        stubFor(WireMock.get(urlMatching("/" + formattedPid + "/full/full/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile("bunny.jpg")
                        .withHeader("Content-Type", "image/jpeg")));

        MvcResult result = mvc.perform(get("/downloadImage/" + pidString + "/full"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        assertEquals("attachment; filename=bunny_full.jpg", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetImageAtPixelSizeSmallerThanFull() throws Exception {
        var pidString = makePid().getId();
        var formattedPid = idToPath(pidString, 4, 2) + pidString + ".jp2";
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        Datastream datastream = mock(Datastream.class);

        stubFor(WireMock.get(urlMatching("/" + formattedPid + "/full/!800,800/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile("bunny.jpg")
                        .withHeader("Content-Type", "image/jpeg")));

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        when(record.getDatastreamObject("original_file")).thenReturn(datastream);
        when(datastream.getExtent()).thenReturn("1200x1200");

        MvcResult result = mvc.perform(get("/downloadImage/" + pidString + "/800"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();

        assertEquals("attachment; filename=bunny_800px.jpg", response.getHeader(CONTENT_DISPOSITION));
        assertCorrectImageReturned(response);
    }

    @Test
    public void testGetImageAtPixelSizeBiggerThanFull() throws Exception {
        var pidString = makePid().getId();
        var formattedPid = idToPath(pidString, 4, 2) + pidString + ".jp2";
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        Datastream datastream = mock(Datastream.class);

        stubFor(WireMock.get(urlMatching("/" + formattedPid + "/full/full/0/default.jpg"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile("bunny.jpg")
                        .withHeader("Content-Type", "image/jpeg")));

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        when(record.getDatastreamObject("original_file")).thenReturn(datastream);
        when(datastream.getExtent()).thenReturn("1200x1200");

        MvcResult result = mvc.perform(get("/downloadImage/" + pidString + "/2500"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();

        assertEquals("attachment; filename=bunny_full.jpg", response.getHeader(CONTENT_DISPOSITION));
        assertCorrectImageReturned(response);
    }

    @Test
    public void testFullSizeAccessImageNoFullSizePermissions() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewOriginal));

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
                .andExpect(status().isForbidden())
                .andReturn();

        var response = result.getResponse();
        assertEquals("Insufficient permissions", response.getContentAsString());
    }

    @Test
    public void testGetImageAtPixelSizeBiggerThanFullNoPermission() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        Datastream datastream = mock(Datastream.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewOriginal));

        when(record.getDatastreamObject("original_file")).thenReturn(datastream);
        when(datastream.getExtent()).thenReturn("1200x1200");

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/2500"))
                .andExpect(status().isForbidden())
                .andReturn();

        var response = result.getResponse();
        assertEquals("Insufficient permissions", response.getContentAsString());
    }

    @Test
    public void testGetImageNoViewAccessCopyPermission() throws Exception {
        var pid = makePid();
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(viewAccessCopies));

        MvcResult result = mvc.perform(get("/downloadImage/" + pid.getId() + "/1200"))
                .andExpect(status().isForbidden())
                .andReturn();

        var message = result.getResponse().getContentAsString();
        assertEquals("Insufficient permissions", message);
    }

    @Test
    public void testAccessImageInvalidSize() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/library"))
                .andExpect(status().isBadRequest())
                .andReturn();

        var message = result.getResponse().getContentAsString();
        assertEquals(message, DownloadImageService.INVALID_SIZE_MESSAGE);
    }

    @Test
    public void testAccessImageNegativeSize() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/-1"))
                .andExpect(status().isBadRequest())
                .andReturn();

        var message = result.getResponse().getContentAsString();
        assertEquals(message, DownloadImageService.INVALID_SIZE_MESSAGE);
    }

    @Test
    public void testAccessImageZeroSize() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/0"))
                .andExpect(status().isBadRequest())
                .andReturn();

        var message = result.getResponse().getContentAsString();
        assertEquals(message, DownloadImageService.INVALID_SIZE_MESSAGE);
    }

    @Test
    public void testGetAccessImageNoOriginalFile() throws Exception {
        var pidString = makePid().getId();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        when(record.getDatastreamObject("original_file")).thenReturn(null);

        MvcResult result = mvc.perform(get("/downloadImage/" + pidString + "/1200"))
                .andExpect(status().isBadRequest())
                .andReturn();

        var message = result.getResponse().getContentAsString();
        assertEquals(message, DownloadImageService.INVALID_SIZE_MESSAGE);
    }

    private void assertCorrectImageReturned(MockHttpServletResponse response) throws IOException {
        assertEquals("image/jpeg", response.getContentType());

        var responseContent = response.getContentAsByteArray();
        byte[] imageContent = FileUtils.readFileToByteArray(new File("src/test/resources/__files/bunny.jpg"));

        assertArrayEquals(responseContent, imageContent);
    }
}
