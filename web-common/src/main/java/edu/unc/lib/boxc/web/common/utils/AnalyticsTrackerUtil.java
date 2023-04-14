package edu.unc.lib.boxc.web.common.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.matomo.java.tracking.MatomoTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.matomo.java.tracking.MatomoRequest;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;

/**
 * Utility for performing asynchronous analytics tracking events when unable to use the javascript api
 *
 * @author bbpennel
 * @date Apr 21, 2014
 */
public class AnalyticsTrackerUtil {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTrackerUtil.class);

    // Made up CID to use if the request does not include one, such as from a API request
    protected static final String DEFAULT_CID = "35009a79-1a05-49d7-b876-2b884d0f825b";
    // Google analytics measurement API url
    private static final String GA_URL = "https://www.google-analytics.com/collect";
    private static final int MATOMO_SITE_ID = 3;
    private static final String MATOMO_API_URL = "https://analytics-qa.lib.unc.edu/matomo.php";
    private static final String MATOMO_ACTION = "download";

    // Google analytics tracking id
    private String gaTrackingID;

    private HttpClientConnectionManager httpClientConnectionManager;
    private CloseableHttpClient httpClient;
    private String repositoryHost;
    private String matomoAuthToken;
    private SolrSearchService solrSearchService;

    public void setHttpClientConnectionManager(HttpClientConnectionManager manager) {
        this.httpClientConnectionManager = manager;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(httpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setGaTrackingID(String trackingID) {
        this.gaTrackingID = trackingID;
    }

    /**
     * Track an event with the specified action for object pid for the active user on the request.
     *
     * @param request request
     * @param action action for the event
     * @param pid pid identifying the object involved in the event
     * @param principals authorization principals
     */
    public void trackEvent(HttpServletRequest request, String action, PID pid, AccessGroupSet principals) {
        try {
            AnalyticsUserData userData = new AnalyticsUserData(request);

            ContentObjectRecord briefObject = solrSearchService.getObjectById(new SimpleIdRequest(pid, principals));
            String parentCollection = briefObject.getParentCollection() == null ?
                    "(no collection)"
                    : briefObject.getParentCollectionName();
            String viewedObjectLabel = briefObject.getTitle() + "|" + pid;
            trackEvent(userData, parentCollection, action, viewedObjectLabel);
            // track in matomo
            var matomoRequest = buildMatomoRequest(getFullURL(request), userData, parentCollection, viewedObjectLabel);
            sendMatomoRequest(matomoRequest);
        } catch (Exception e) {
            // Prevent analytics exceptions from impacting user
            log.warn("An exception occurred while recording {} event on {}", action, pid, e);
        }
    }

    private void trackEvent(AnalyticsUserData userData, String category, String action, String label) {
        if (userData == null) {
            return;
        }

        // Perform the analytics tracking event asynchronously
        Thread trackerThread = new Thread(new EventTrackerRunnable(userData, category, action, label));
        trackerThread.start();
    }

    public MatomoRequest buildMatomoRequest(String url, AnalyticsUserData userData, String parentCollection, String label) {
        return MatomoRequest.builder()
                .siteId(MATOMO_SITE_ID)
                .visitorId(userData.uid)
                .actionUrl(url)
                .actionName(MATOMO_ACTION)
                .downloadUrl(url)
                .eventCategory(parentCollection)
                .eventAction(MATOMO_ACTION)
                .eventName(label)
                .headerUserAgent(userData.userAgent)
                .authToken(matomoAuthToken)
                .visitorIp(userData.uip)
                .build();
    }

    private void sendMatomoRequest(MatomoRequest matomoRequest) {
        var tracker = new MatomoTracker(MATOMO_API_URL);

        try {
            Future<HttpResponse> response = tracker.sendRequestAsync(matomoRequest);
        } catch (Exception e) {
            log.warn("Error while sending request for download event at {} to Matomo", matomoRequest.getDownloadUrl());
        }
    }

    private String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    /**
     * @param solrSearchService the solrSearchService to set
     */
    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setRepositoryHost(String repositoryHost) {
        this.repositoryHost = repositoryHost;
    }
    public void setMatomoAuthToken(String matomoAuthToken) {
        this.matomoAuthToken = matomoAuthToken;
    }

    public static class AnalyticsUserData {
        // ip of client
        public String uip;
        // client id
        public String cid;
        public String userAgent;
        // matomo user id
        public String uid;

        public AnalyticsUserData(HttpServletRequest request) {

            // Get the user's IP address, either from proxy headers or request
            uip = request.getHeader("X-Forwarded-For");
            if (uip == null || uip.length() == 0 || "unknown".equalsIgnoreCase(uip)) {
                uip = request.getHeader("Proxy-Client-IP");
            }
            if (uip == null || uip.length() == 0 || "unknown".equalsIgnoreCase(uip)) {
                uip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (uip == null || uip.length() == 0 || "unknown".equalsIgnoreCase(uip)) {
                uip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (uip == null || uip.length() == 0 || "unknown".equalsIgnoreCase(uip)) {
                uip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (uip == null || uip.length() == 0 || "unknown".equalsIgnoreCase(uip)) {
                uip = request.getRemoteAddr();
            }

            // Store the CID from _ga cookie if it is present
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    // if both cookies have been found
                    if (cid != null && uid != null) {
                        break;
                    }
                    if ("_ga".equals(cookie.getName())) {
                        String[] parts = cookie.getValue().split("\\.");
                        if (parts.length == 4) {
                            cid = parts[2] + "." + parts[3];
                        }
                    } else if ("_pk_id".equals(cookie.getName())) {
                        String[] parts = cookie.getValue().split("\\.");
                        uid = parts[0];
                    }
                }
            }

            if (cid == null) {
                cid = DEFAULT_CID;
            }

            userAgent = request.getHeader("User-Agent");
            if (userAgent == null) {
                userAgent = "";
            }
        }
    }

    protected class EventTrackerRunnable implements Runnable {

        private final AnalyticsUserData userData;
        private final String category;
        private final String action;
        private final String label;

        public EventTrackerRunnable(AnalyticsUserData userData, String category, String action, String label) {
            this.category = category;
            this.action = action;
            this.label = label;
            this.userData = userData;
        }

        @Override
        public void run() {
            log.debug("Tracking user {} with event {} in category {} with label {}",
                    userData.cid, action, category, label);

            URIBuilder builder;
            try {
                builder = new URIBuilder(GA_URL);
            } catch (URISyntaxException e) {
                log.warn("Failed to build URI for tracker", e);
                return;
            }

            // See https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("v", "1"));
            params.add(new BasicNameValuePair("tid", gaTrackingID));
            params.add(new BasicNameValuePair("cid", userData.cid));
            params.add(new BasicNameValuePair("t", "event"));
            params.add(new BasicNameValuePair("uip", userData.uip));
            params.add(new BasicNameValuePair("ua", userData.userAgent));
            params.add(new BasicNameValuePair("dh", repositoryHost));
            params.add(new BasicNameValuePair("an", "cdr"));
            params.add(new BasicNameValuePair("de", "UTF-8"));
            params.add(new BasicNameValuePair("ul", "en-us"));
            log.debug("Tracking user {} with event {} in category {} with label {}",
                    userData.cid, action, category, label);
            log.debug("Tracking:{} {} {} {}", new Object[] { GA_URL, gaTrackingID, userData.cid, userData.uip});

            if (category != null) {
                params.add(new BasicNameValuePair("ec", category));
            }
            if (action != null) {
                params.add(new BasicNameValuePair("ea", action));
            }
            if (label != null) {
                params.add(new BasicNameValuePair("el", label));
            }

            builder.addParameters(params);

            HttpGet method;
            try {
                URI url = builder.build();
                method = new HttpGet(url);
                method.addHeader("Accept", "*/*");
            } catch (URISyntaxException e) {
                log.warn("Failed to build tracking url", e);
                return;
            }

            try (CloseableHttpResponse resp = httpClient.execute(method)) {
            } catch (Exception e) {
                log.warn("Failed to issue tracking event for cid {}", e, userData.cid);
            }
        }

    }
}
