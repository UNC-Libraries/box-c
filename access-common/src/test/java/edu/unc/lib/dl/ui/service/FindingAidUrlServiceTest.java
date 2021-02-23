/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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

    @Before
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
}
