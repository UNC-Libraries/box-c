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
package edu.unc.lib.dl.ui.util;

import javax.annotation.PreDestroy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for performing asynchronous analytics tracking events when unable to use the javascript api
 *
 * @author bbpennel
 * @date Apr 21, 2014
 */
public class AnalyticsTrackerUtil {

	private static final Logger log = LoggerFactory.getLogger(AnalyticsTrackerUtil.class);

	// Made up CID to use if the request does not include one, such as from a API request
	private final String DEFAULT_CID = "35009a79-1a05-49d7-b876-2b884d0f825b";
	// Google analytics measurement API url
	private final String GA_URL = "http://www.google-analytics.com/collect";

	// Google analytics tracking id
	private String gaTrackingID;

	private final MultiThreadedHttpConnectionManager httpManager;
	private final HttpClient httpClient;

	public AnalyticsTrackerUtil() {

		// Use a threaded manager with timeouts
		httpManager = new MultiThreadedHttpConnectionManager();
		httpManager.getParams().setConnectionTimeout(2000);

		httpClient = new HttpClient(httpManager);
	}

	@PreDestroy
	public void destroy() {
		httpManager.shutdown();
	}

	public void setGaTrackingID(String trackingID) {
		this.gaTrackingID = trackingID;
	}

	public void trackEvent(String cid, String category, String action, String label, Integer value) {

		// Use a default customer ID if none was provided, since it is required
		if (cid == null)
			cid = DEFAULT_CID;

		// Perform the analytics tracking event asynchronously
		Thread trackerThread = new Thread(new EventTrackerRunnable(cid, category, action, label, value));
		trackerThread.start();
	}

	private class EventTrackerRunnable implements Runnable {

		private final String category;
		private final String action;
		private final String label;
		private final Integer value;
		private final String cid;

		public EventTrackerRunnable(String cid, String category, String action, String label,
				Integer value) {
			this.category = category;
			this.action = action;
			this.label = label;
			this.value = value;
			this.cid = cid;
		}

		@Override
		public void run() {
			if (log.isDebugEnabled())
				log.debug("Tracking user {} with event {} in category {} with label {}",
						new String[] { cid, action, category, label });

			PostMethod method = new PostMethod(GA_URL);
			method.setParameter("v", "1");
			method.setParameter("tid", gaTrackingID);
			method.setParameter("cid", cid);
			method.setParameter("t", "event");

			if (category != null)
				method.setParameter("ec", category);
			if (action != null)
				method.setParameter("ea", action);
			if (label != null)
				method.setParameter("el", label);
			if (value != null)
				method.setParameter("ev", value.toString());

			try {
				httpClient.executeMethod(method);
			} catch (Exception e) {
				log.warn("Failed to issue tracking event for cid {}", e, cid);
			} finally {
				method.releaseConnection();
			}
		}

	}
}
