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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static edu.unc.lib.boxc.web.common.utils.StringFormatUtil.urlEncode;
import static java.util.Map.entry;
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
        principals = new AccessGroupSetImpl("some_group");
    }

    @Test
    public void testTrackEventInCollection() throws Exception {
        when(request.getHeader("Proxy-Client-IP")).thenReturn("0.0.0.0");
        var cidCookie = mock(Cookie.class);
        when(cidCookie.getName()).thenReturn("_ga");
        when(cidCookie.getValue()).thenReturn("ga.1.123456789.1234567890");
        var uidCookie = mock(Cookie.class);
        var userId = "5e462bae5cada463";
        when(uidCookie.getName()).thenReturn("_pk_id");
        when(uidCookie.getValue()).thenReturn(userId +".1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ cidCookie, uidCookie });
        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
        var urlBuffer = new StringBuffer("https://www.example.org");
        when(request.getRequestURL()).thenReturn(urlBuffer);
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work");
        when(contentObjectRecord.getParentCollection()).thenReturn("parent_coll");
        when(contentObjectRecord.getParentCollectionName()).thenReturn("Parent Collection");

        var params = buildParams(userId, urlBuffer.toString(), true);

        stubFor(WireMock.get(urlPathEqualTo("/")).withQueryParams(params)
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        analyticsTrackerUtil.trackEvent(request, "testAction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());
        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertGaQueryIsCorrect(gaUri, true);

        assertMatomoQueryIsCorrect(userId, urlBuffer.toString());
    }

    @Test
    public void testTrackEventNotInCollection() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work2");
        when(contentObjectRecord.getParentCollection()).thenReturn(null);
        var uidCookie = mock(Cookie.class);
        var userId = "5e462bae5cada463";
        when(uidCookie.getName()).thenReturn("_pk_id");
        when(uidCookie.getValue()).thenReturn(userId + ".1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ uidCookie });
        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
        var urlBuffer = new StringBuffer("https://www.example.org");
        when(request.getRequestURL()).thenReturn(urlBuffer);

        var params = buildParams(userId, urlBuffer.toString(), false);

        stubFor(WireMock.get(urlPathEqualTo("/")).withQueryParams(params)
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        analyticsTrackerUtil.trackEvent(request, "testAction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());

        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertGaQueryIsCorrect(gaUri, false);
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
            assertTrue(gaQuery.contains("&el=Test+Work|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
        } else {
            assertTrue(gaQuery.contains("&cid=" + AnalyticsTrackerUtil.DEFAULT_CID));
            assertTrue(gaQuery.contains("&ec=(no+collection)"));
            assertTrue(gaQuery.contains("&uip=1.1.1.1"));
            assertTrue(gaQuery.contains("&el=Test+Work2|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
        }
    }

    private void assertMatomoQueryIsCorrect(String userId, String urlBuffer) throws UnsupportedEncodingException {
        var url = urlEncode(urlBuffer);
        for (int i=0 ; i<100 ; i++) {
            try {
                Thread.sleep(10);
                WireMock.verify(getRequestedFor(urlPathEqualTo("/"))
                        .withQueryParam("_id", equalTo(userId))
                        .withQueryParam("action_name", equalTo("download"))
                        .withQueryParam("cip", equalTo("0.0.0.0"))
                        .withQueryParam("idsite", equalTo("3"))
                        .withQueryParam("token_auth", equalTo(urlEncode(authToken)))
                        .withQueryParam("ua", equalTo("boxy-client"))
                        .withQueryParam("url", equalTo(url))
                        .withQueryParam("download", equalTo(url))
                        .withQueryParam("e_a", equalTo("download"))
                        .withQueryParam("e_c", equalTo("Parent+Collection"))
                );
                return;
            } catch (VerificationException | InterruptedException ignored) {
            }
        }
        throw new VerificationException("The query was not correct");
    }

    Map<String, StringValuePattern> buildParams(String userId, String urlBuffer, boolean withCollection) throws UnsupportedEncodingException {
        Map<String, StringValuePattern> params = new HashMap<>();
        params.put("_id", equalTo(userId));
        params.put("action_name", equalTo("download"));
        params.put("idsite", equalTo("3"));
        params.put("token_auth", equalTo(urlEncode(authToken)));
        params.put("ua", equalTo("boxy-client"));
        params.put("url", equalTo(urlEncode(urlBuffer)));
        params.put("download", equalTo(urlEncode(urlBuffer)));
        params.put("e_a", equalTo("download"));


        if (withCollection) {
            params.put("cip", equalTo("0.0.0.0"));
            params.put("e_c", equalTo("Parent+Collection"));
            params.put("e_n", equalTo("Test+Work|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
        } else {
            params.put("cip", equalTo("1.1.1.1"));
            params.put("e_c", equalTo("(no+collection)"));
            params.put("e_n", equalTo("Test+Work2|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
        }
        return params;
    }
}
