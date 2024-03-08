package edu.unc.lib.boxc.web.common.utils;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@WireMockTest(httpPort = 46887)
public class AnalyticsTrackerUtilTest {
    private final static String PID_UUID = "03114533-0017-4c83-b9d9-567b08fb2429";
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ContentObjectRecord contentObjectRecord;
    private AccessGroupSet principals;

    private AnalyticsTrackerUtil analyticsTrackerUtil;
    private String authToken = "secret123456789qwertyasdfghzxcvb";
    private String apiURL = "http://localhost:46887";
    private String userId = "5e462bae5cada463";
    private StringBuffer urlBuffer = new StringBuffer("https://www.example.org");
    private String url = "http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429";
    private int siteID = 3;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        analyticsTrackerUtil = new AnalyticsTrackerUtil();
        analyticsTrackerUtil.setSolrSearchService(solrSearchService);
        analyticsTrackerUtil.setMatomoApiURL(apiURL);
        analyticsTrackerUtil.setMatomoAuthToken(authToken);
        analyticsTrackerUtil.setMatomoSiteID(siteID);
        principals = new AccessGroupSetImpl("some_group");
    }

    @Test
    public void testTrackEventInCollection() throws Exception {
        when(request.getHeader("Proxy-Client-IP")).thenReturn("0.0.0.0");
        var uidCookie = mock(Cookie.class);
        when(uidCookie.getName()).thenReturn("_pk_id");
        when(uidCookie.getValue()).thenReturn(userId +".1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ uidCookie });
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

//    @Test
//    public void testAnalyticsUserDataInvalidUidCookie() {
//        when(request.getHeader("Proxy-Client-IP")).thenReturn("0.0.0.0");
//        var uidCookie = mock(Cookie.class);
//        when(uidCookie.getName()).thenReturn("_pk_id");
//        when(uidCookie.getValue()).thenReturn("");
//        when(request.getCookies()).thenReturn(new Cookie[]{ uidCookie });
//        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
//        when(request.getRequestURL()).thenReturn(urlBuffer);
//
//        var userData = new AnalyticsTrackerUtil.AnalyticsUserData(request);
//        assertNotNull(userData.uid);
//        assertFalse(userData.uid.isEmpty());
//    }

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
        params.put("idsite", Integer.toString(siteID));
        params.put("token_auth", URLEncoder.encode(authToken, StandardCharsets.UTF_8.toString()));
        params.put("ua", "boxy-client");
        params.put("url", urlBuffer.toString());
        params.put("e_a", "Downloaded Original");
        var formattedAction = " / " + AnalyticsTrackerUtil.MATOMO_ACTION;

        if (withCollection) {
            var collectionName = "Parent Collection";
            params.put("cip", "0.0.0.0");
            params.put("e_c", collectionName);
            params.put("action_name", collectionName + formattedAction);
            params.put("e_n", "Test Work|" + url);
        } else {
            var collectionName = "(no collection)";
            params.put("cip", "1.1.1.1");
            params.put("e_c", collectionName);
            params.put("action_name", collectionName + formattedAction);
            params.put("e_n", "Test Work2|" + url);
        }
        return params;
    }
}
