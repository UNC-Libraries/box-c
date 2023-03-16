package edu.unc.lib.boxc.web.common.utils;

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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

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

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        analyticsTrackerUtil = new AnalyticsTrackerUtil();
        analyticsTrackerUtil.setRepositoryHost(repositoryHost);
        analyticsTrackerUtil.setGaTrackingID(trackingId);
        analyticsTrackerUtil.setSolrSearchService(solrSearchService);
        analyticsTrackerUtil.setHttpClient(httpClient);
        principals = new AccessGroupSetImpl("some_group");
    }

    @Test
    public void testTrackEventInCollection() throws Exception {
        when(request.getHeader("Proxy-Client-IP")).thenReturn("0.0.0.0");
        var cidCookie = mock(Cookie.class);
        when(cidCookie.getName()).thenReturn("_ga");
        when(cidCookie.getValue()).thenReturn("ga.1.123456789.1234567890");
        when(request.getCookies()).thenReturn(new Cookie[]{ cidCookie });
        when(request.getHeader("User-Agent")).thenReturn("boxy-client");
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work");
        when(contentObjectRecord.getParentCollection()).thenReturn("parent_coll");
        when(contentObjectRecord.getParentCollectionName()).thenReturn("Parent Collection");

        analyticsTrackerUtil.trackEvent(request, "testAction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());

        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertEquals("www.google-analytics.com", gaUri.getHost());
        assertEquals("/collect", gaUri.getPath());
        var gaQuery = gaUri.getQuery();
        assertTrue(gaQuery.contains("&cid=123456789.1234567890"));
        assertTrue(gaQuery.contains("&t=event"));
        assertTrue(gaQuery.contains("&uip=0.0.0.0"));
        assertTrue(gaQuery.contains("&ua=boxy-client"));
        assertTrue(gaQuery.contains("&dh=boxy.example.com"));
        assertTrue(gaQuery.contains("&an=cdr"));
        assertTrue(gaQuery.contains("&de=UTF-8"));
        assertTrue(gaQuery.contains("&ec=Parent+Collection"));
        assertTrue(gaQuery.contains("&ea=testAction"));
        assertTrue(gaQuery.contains("&el=Test+Work|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
    }

    @Test
    public void testTrackEventNotInCollection() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        var pid = PIDs.get(PID_UUID);
        when(solrSearchService.getObjectById(any())).thenReturn(contentObjectRecord);
        when(contentObjectRecord.getTitle()).thenReturn("Test Work2");
        when(contentObjectRecord.getParentCollection()).thenReturn(null);

        analyticsTrackerUtil.trackEvent(request, "testTraction", pid, principals);

        verify(httpClient, timeout(1000).times(1)).execute(httpRequestCaptor.capture());

        var gaRequest = httpRequestCaptor.getValue();
        var gaUri = gaRequest.getURI();
        assertEquals("www.google-analytics.com", gaUri.getHost());
        assertEquals("/collect", gaUri.getPath());
        var gaQuery = gaUri.getQuery();
        assertTrue(gaQuery.contains("&cid=" + AnalyticsTrackerUtil.DEFAULT_CID));
        assertTrue(gaQuery.contains("&t=event"));
        assertTrue(gaQuery.contains("&uip=1.1.1.1"));
        assertTrue(gaQuery.contains("&ua="));
        assertTrue(gaQuery.contains("&dh=boxy.example.com"));
        assertTrue(gaQuery.contains("&an=cdr"));
        assertTrue(gaQuery.contains("&de=UTF-8"));
        assertTrue(gaQuery.contains("&ec=(no+collection)"));
        assertTrue(gaQuery.contains("&ea=testTraction"));
        assertTrue(gaQuery.contains("&el=Test+Work2|http://example.com/rest/content/03/11/45/33/03114533-0017-4c83-b9d9-567b08fb2429"));
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
}
