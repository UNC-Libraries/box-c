package edu.unc.lib.boxc.web.common.utils;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.springframework.http.HttpStatus;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static edu.unc.lib.boxc.web.common.utils.StringFormatUtil.urlEncode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@WireMockTest(httpPort = 46887)
public class AnalyticsTrackerUtilTest {
    private final static String PID_UUID = "03114533-0017-4c83-b9d9-567b08fb2429";
    @Mock
    private HttpClientConnectionManager httpClientConnectionManager;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ContentObjectRecord contentObjectRecord;
    @Captor
    private ArgumentCaptor<HttpUriRequest> httpRequestCaptor;
    private AccessGroupSet principals;

    private AnalyticsTrackerUtil analyticsTrackerUtil;
    private String repositoryHost = "boxy.example.com";
    private String trackingId = "trackme";
    private String authToken = "secret123456789qwertyasdfghzxcvb";
    private String apiURL = "http://localhost:46887";
    private String userId = "5e462bae5cada463";
    private StringBuffer urlBuffer = new StringBuffer("https://www.example.org");
    private String url = "http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429";
    private String siteID = "1";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        analyticsTrackerUtil = new AnalyticsTrackerUtil();
        analyticsTrackerUtil.setRepositoryHost(repositoryHost);
        analyticsTrackerUtil.setGaTrackingID(trackingId);
        analyticsTrackerUtil.setSolrSearchService(solrSearchService);
        analyticsTrackerUtil.setHttpClient(httpClient);
        analyticsTrackerUtil.setMatomoApiURL(apiURL);
        analyticsTrackerUtil.setMatomoAuthToken(authToken);
        analyticsTrackerUtil.setMatomoSiteID(Integer.parseInt(siteID));
        principals = new AccessGroupSetImpl("some_group");
    }

    @Test
    public void testTrackEventInCollection() throws Exception {
        when(request.getHeader("Proxy-Client-IP")).thenReturn("0.0.0.0");
        var cidCookie = mock(Cookie.class);
        when(cidCookie.getName()).thenReturn("_ga");
        when(cidCookie.getValue()).thenReturn("ga.1.123456789.1234567890");
        var uidCookie = mock(Cookie.class);
        when(uidCookie.getName()).thenReturn("_pk_id");
        when(uidCookie.getValue()).thenReturn(userId +".1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ cidCookie, uidCookie });
        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
        when(request.getRequestURL()).thenReturn(urlBuffer);
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work");
        when(contentObjectRecord.getParentCollection()).thenReturn("parent_coll");
        when(contentObjectRecord.getParentCollectionName()).thenReturn("Parent Collection");

        var stringParams = buildStringParams(true);
        var expectedParams = buildPatternParams(stringParams);

        stubFor(WireMock.get(urlPathEqualTo("/")).withQueryParams(expectedParams)
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        analyticsTrackerUtil.trackEvent(request, "testAction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());
        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertGaQueryIsCorrect(gaUri, true);

        assertMatomoQueryIsCorrect(expectedParams);
    }

    @Test
    public void testTrackEventNotInCollection() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work2");
        when(contentObjectRecord.getParentCollection()).thenReturn(null);
        var uidCookie = mock(Cookie.class);
        when(uidCookie.getName()).thenReturn("_pk_id");
        when(uidCookie.getValue()).thenReturn(userId + ".1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ uidCookie });
        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
        when(request.getRequestURL()).thenReturn(urlBuffer);

        var stringParams = buildStringParams(false);
        var expectedParams = buildPatternParams(stringParams);

        stubFor(WireMock.get(urlPathEqualTo("/")).withQueryParams(expectedParams)
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        analyticsTrackerUtil.trackEvent(request, "testAction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());

        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertGaQueryIsCorrect(gaUri, false);

        assertMatomoQueryIsCorrect(expectedParams);
    }

    @Test
    public void testAnalyticsUserDataWlProxyClientIP() throws Exception {
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("1.0.0.0");
        var userData = new AnalyticsTrackerUtil.AnalyticsUserData(request);
        assertEquals("1.0.0.0", userData.uip);
    }

    @Test
    public void testAnalyticsUserDataHttpClientIp() throws Exception {
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("1.1.0.0");
        var userData = new AnalyticsTrackerUtil.AnalyticsUserData(request);
        assertEquals("1.1.0.0", userData.uip);
    }

    @Test
    public void testAnalyticsUserDataHttpXForwardedFor() throws Exception {
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("1.1.1.0");
        var userData = new AnalyticsTrackerUtil.AnalyticsUserData(request);
        assertEquals("1.1.1.0", userData.uip);
    }

    @Test
    public void testAnalyticsUserDataRemoteAddr() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.1.1.1");
        var userData = new AnalyticsTrackerUtil.AnalyticsUserData(request);
        assertEquals("1.1.1.1", userData.uip);
    }

    @Test
    public void testSetHttpClientConnectionManager() throws Exception {
        // Just verifying the setter doesn't error
        analyticsTrackerUtil.setHttpClientConnectionManager(httpClientConnectionManager);
    }

    private void assertGaQueryIsCorrect(URI gaUri, boolean withCollection) {
        assertEquals("www.google-analytics.com", gaUri.getHost());
        assertEquals("/collect", gaUri.getPath());
        var gaQuery = gaUri.getQuery();
        assertTrue(gaQuery.contains("&t=event"));
        assertTrue(gaQuery.contains("&ua=boxy-client"));
        assertTrue(gaQuery.contains("&dh=boxy.example.com"));
        assertTrue(gaQuery.contains("&an=cdr"));
        assertTrue(gaQuery.contains("&de=UTF-8"));
        assertTrue(gaQuery.contains("&ea=testAction"));

        if (withCollection) {
            assertTrue(gaQuery.contains("&cid=123456789.1234567890"));
            assertTrue(gaQuery.contains("&ec=Parent+Collection"));
            assertTrue(gaQuery.contains("&uip=0.0.0.0"));
            assertTrue(gaQuery.contains("&el=Test+Work|" + url));
        } else {
            assertTrue(gaQuery.contains("&cid=" + AnalyticsTrackerUtil.DEFAULT_CID));
            assertTrue(gaQuery.contains("&ec=(no+collection)"));
            assertTrue(gaQuery.contains("&uip=1.1.1.1"));
            assertTrue(gaQuery.contains("&el=Test+Work2|" + url));
        }
    }

    private void assertMatomoQueryIsCorrect(Map<String, StringValuePattern> params) {
        for (int i=0 ; i<100 ; i++) {
            try {
                Thread.sleep(10);
                WireMock.verify(getRequestedFor(urlPathEqualTo("/"))
                        .withQueryParam("_id", params.get("_id"))
                        .withQueryParam("action_name", params.get("action_name"))
                        .withQueryParam("cip", params.get("cip"))
                        .withQueryParam("idsite", params.get("idsite"))
                        .withQueryParam("token_auth", params.get("token_auth"))
                        .withQueryParam("ua", params.get("ua"))
                        .withQueryParam("url", params.get("url"))
                        .withQueryParam("e_a", params.get("e_a"))
                        .withQueryParam("e_c", params.get("e_c"))
                );
                return;
            } catch (VerificationException | InterruptedException ignored) {
            }
        }
        throw new VerificationException("The query was not correct");
    }

    private Map<String, StringValuePattern> buildPatternParams(Map<String, String> stringParams) {
        Map<String, StringValuePattern> params = new HashMap<>();
        for (String key : stringParams.keySet()) {
            params.put(key, equalTo(stringParams.get(key)));
        }

        return params;
    }

    private Map<String, String> buildStringParams(boolean withCollection) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        params.put("_id", userId);
        params.put("idsite", siteID);
        params.put("token_auth", urlEncode(authToken));
        params.put("ua", "boxy-client");
        params.put("url", urlEncode(urlBuffer.toString()));
        params.put("e_a", "Downloaded+Original");
        var formattedAction = urlEncode(" / " + AnalyticsTrackerUtil.MATOMO_ACTION);

        if (withCollection) {
            var collectionName = "Parent+Collection";
            params.put("cip", "0.0.0.0");
            params.put("e_c", collectionName);
            params.put("action_name", collectionName + formattedAction);
            params.put("e_n", "Test+Work|" + url);
        } else {
            var collectionName = urlEncode("(") + "no+collection" + urlEncode(")");
            params.put("cip", "1.1.1.1");
            params.put("e_c", collectionName);
            params.put("action_name", collectionName + formattedAction);
            params.put("e_n", "Test+Work2|" + url);
        }
        return params;
    }
}
