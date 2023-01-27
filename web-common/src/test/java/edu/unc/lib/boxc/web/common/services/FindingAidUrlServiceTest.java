package edu.unc.lib.boxc.web.common.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.web.common.services.FindingAidUrlService;

/**
 * @author bbpennel
 */
public class FindingAidUrlServiceTest {

    private FindingAidUrlService service;

    private static final String BASE_URL = "http://example.com/";

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResp;
    @Mock
    private StatusLine statusLine;

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);
        service = new FindingAidUrlService();
        service.setHttpClient(httpClient);
        service.setFindingAidBaseUrl(BASE_URL);
        service.setExpireCacheSeconds(5);
        service.setMaxCacheSize(16);
        service.init();

        when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResp);
        when(httpResp.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void nullCollectionIdTest() {
        assertNull(service.getFindingAidUrl(null));
    }

    @Test
    public void existingCollectionIdTest() {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        String collId = "55555";
        assertEquals(BASE_URL + collId + "/", service.getFindingAidUrl(collId));
    }

    @Test
    public void notFoundCollectionIdTest() {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        String collId = "40404";
        assertNull(service.getFindingAidUrl(collId));
    }

    @Test
    public void failureCollectionIdTest() {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        String collId = "50000";
        assertNull(service.getFindingAidUrl(collId));
    }

    @Test
    public void unencodedCollectionIdTest() {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        String collId = "55555 oh+no";
        assertEquals(BASE_URL + "55555%20oh+no/", service.getFindingAidUrl(collId));
    }

    @Test
    public void blankCollectionIdTest() {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        String collId = "";
        assertNull(service.getFindingAidUrl(collId));
    }

    @Test
    public void noBaseUrlTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.setFindingAidBaseUrl(null);
            service.init();
        });
    }
}
