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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.unc.lib.dl.util.URIUtil;

/**
 * Service for getting finding aid URLs
 *
 * @author bbpennel
 */
public class FindingAidUrlService {
    private static final Logger log = getLogger(FindingAidUrlService.class);

    private LoadingCache<String, Optional<String>> collIdTolinkCache;

    private long maxCacheSize;
    private long expireCacheSeconds;
    private int checkTimeoutSeconds = 5;
    private String findingAidBaseUrl;
    private CloseableHttpClient httpClient;

    public void init() {
        collIdTolinkCache = CacheBuilder.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(expireCacheSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Optional<String>>() {
                    @Override
                    public Optional<String> load(String key) throws Exception {
                        String baseCollId = key.replace("-z", "");
                        String url = URIUtil.join(findingAidBaseUrl, baseCollId) + "/";
                        HttpHead req = new HttpHead(url);
                        try (CloseableHttpResponse resp = httpClient.execute(req)) {
                            StatusLine statusLine = resp.getStatusLine();
                            int status = statusLine.getStatusCode();
                            if (status < HttpStatus.SC_BAD_REQUEST) {
                                return Optional.of(url);
                            } else if (status == HttpStatus.SC_NOT_FOUND) {
                                log.debug("Finding aid not found for collection {} at {}", key, url);
                                return Optional.empty();
                            } else {
                                log.error("Failed to check on finding aid URL {}: {} {}",
                                        url, statusLine.getStatusCode(), statusLine.getReasonPhrase());
                                return Optional.empty();
                            }
                        }
                    }
                });
    }

    /**
     * @param collectionId Collection number (not the PID)
     * @return the finding aid URL for the given collection ID, as a string, or null if none found.
     */
    public String getFindingAidUrl(String collectionId) {
        if (StringUtils.isEmpty(collectionId)) {
            return null;
        }
        long start = System.nanoTime();
        try {
            return collIdTolinkCache.get(collectionId).orElse(null);
        } catch (ExecutionException e) {
            log.error("Failed to get finding aid link for {}", collectionId, e);
            return null;
        } finally {
            log.error("Retrieved finding aid URL in {}ms", (System.nanoTime() - start) / 1e6);
        }
    }

    public void setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public void setExpireCacheSeconds(long expireCacheSeconds) {
        this.expireCacheSeconds = expireCacheSeconds;
    }

    public void setCheckTimeoutSeconds(int checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    public void setFindingAidBaseUrl(String findingAidBaseUrl) {
        this.findingAidBaseUrl = findingAidBaseUrl;
    }

    public void setHttpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(checkTimeoutSeconds * 1000)
                .setSocketTimeout(checkTimeoutSeconds * 1000)
                .build();
        httpClient = HttpClients.custom()
                .setConnectionManager(httpClientConnectionManager)
                .setDefaultRequestConfig(config)
                .build();
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
