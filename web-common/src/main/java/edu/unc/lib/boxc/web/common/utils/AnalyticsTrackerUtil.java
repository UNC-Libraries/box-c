package edu.unc.lib.boxc.web.common.utils;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.lang3.StringUtils;
import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.matomo.java.tracking.parameters.VisitorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * Utility for performing asynchronous analytics tracking events when unable to use the javascript api
 *
 * @author bbpennel
 * @date Apr 21, 2014
 */
public class AnalyticsTrackerUtil {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTrackerUtil.class);

    public static final String MATOMO_ACTION = "Downloaded Original";

    private String matomoAuthToken;
    private String matomoApiURL;
    private int matomoSiteID;
    private SolrSearchService solrSearchService;
    private MatomoTracker tracker;

    public void init() {
        tracker = new MatomoTracker(TrackerConfiguration
                .builder()
                .apiEndpoint(URI.create(matomoApiURL))
                .build());
    }

    public void close() {
        try {
            tracker.close();
        } catch (Exception e) {
            log.warn("Failed to close matomo tracker", e);
        }
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
            // track in matomo
            var matomoRequest = buildMatomoRequest(getFullURL(request), userData, parentCollection, viewedObjectLabel);
            sendMatomoRequest(matomoRequest);
        } catch (Exception e) {
            // Prevent analytics exceptions from impacting user
            log.warn("An exception occurred while recording {} event on {}", action, pid, e);
        }
    }

    private MatomoRequest buildMatomoRequest(String url, AnalyticsUserData userData, String parentCollection, String label) throws UnsupportedEncodingException {
        return MatomoRequest.request()
                .siteId(matomoSiteID)
                .visitorId(userData.getVisitorId())
                .actionUrl(url)
                .actionName(parentCollection + " / " + MATOMO_ACTION)
                .eventCategory(parentCollection)
                .eventAction(MATOMO_ACTION)
                .eventName(label)
                .headerUserAgent(userData.userAgent)
                .authToken(matomoAuthToken)
                .visitorIp(userData.uip)
                .build();
    }

    private void sendMatomoRequest(MatomoRequest matomoRequest) {
        try {
            tracker.sendRequestAsync(matomoRequest);
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
    public void setMatomoAuthToken(String matomoAuthToken) {
        this.matomoAuthToken = matomoAuthToken;
    }
    public void setMatomoApiURL(String matomoApiURL) {
        this.matomoApiURL = matomoApiURL;
    }

    public void setMatomoSiteID(int matomoSiteID) {
        this.matomoSiteID = matomoSiteID;
    }

    public static class AnalyticsUserData {
        // ip of client
        public String uip;
        public String userAgent;
        // matomo user id
        public String uid;

        public AnalyticsUserData(HttpServletRequest request) {

            // Get the user's IP address, either from proxy headers or request
            uip = request.getHeader("X-Forwarded-For");
            if (hasUnknownUip(uip)) {
                uip = request.getHeader("Proxy-Client-IP");
            }
            if (hasUnknownUip(uip)) {
                uip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (hasUnknownUip(uip)) {
                uip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (hasUnknownUip(uip)) {
                uip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (hasUnknownUip(uip)) {
                uip = request.getRemoteAddr();
            }

            // Store the user ids from cookie if it is present
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    // if cookie has been found
                    if (uid != null) {
                        break;
                    }
                    var cookieName = cookie.getName();
                    if (cookieName.startsWith("_pk_id")) {
                        // matomo cookie
                        String[] parts = cookie.getValue().split("\\.");
                        uid = parts[0];
                    }
                }
            }

            userAgent = request.getHeader("User-Agent");
            if (userAgent == null) {
                userAgent = "";
            }
        }

        public VisitorId getVisitorId() {
            if (StringUtils.isBlank(uid)) {
                // Generate a random visitor id if none is set
                return VisitorId.random();
            } else {
                return VisitorId.fromHex(uid);
            }
        }

        private boolean hasUnknownUip(String uip) {
            return StringUtils.isBlank(uip) || "unknown".equalsIgnoreCase(uip);
        }
    }
}
