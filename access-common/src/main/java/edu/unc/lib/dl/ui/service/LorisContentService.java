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

import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.ui.exception.ClientAbortException;
import edu.unc.lib.dl.ui.util.ApplicationPathSettings;
import edu.unc.lib.dl.ui.util.FileIOUtil;

/**
 * Generates request, connects to, and streams the output from loris.  Sets pertinent headers.
 * @author bbpennel
 */
public class LorisContentService {
    private static final Logger LOG = LoggerFactory.getLogger(LorisContentService.class);

    @Autowired
    private ApplicationPathSettings applicationPathSettings;

    public void getMetadata(String simplepid, String datastream, OutputStream outStream, HttpServletResponse response) {
        this.getMetadata(simplepid, datastream, outStream, response, 1);
    }

    public void getMetadata(String simplepid, String datastream, OutputStream outStream,
            HttpServletResponse response, int retryServerError) {
        CloseableHttpClient client = HttpClients.createDefault();

        StringBuilder path = new StringBuilder(applicationPathSettings.getLorisPath());
        path.append(simplepid);

        HttpGet method = new HttpGet(path.toString());
        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                if (response != null ) {
                    response.setHeader("Content-Type", "application/json");
                    response.setHeader("content-disposition", "inline");

                    FileIOUtil.stream(outStream, httpResp);
                }
            } else {
                if ((statusCode == 500 || statusCode == 404) && retryServerError > 0 ) {
                    this.getMetadata(simplepid, datastream, outStream, response, retryServerError - 1);
                } else {
                    LOG.error("Unexpected failure: " + httpResp.getStatusLine().toString());
                    LOG.error("Path was: " + method.getURI());
                }
            }
        } catch (ClientAbortException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User client aborted request to stream jp2 metadata for " + simplepid, e);
            }
        } catch (Exception e ) {
            LOG.error("Problem retrieving metadata for " + path, e);
        }
    }

    public void streamJP2(String simplepid, String region, String size, String rotatation, String quality,
            String format, String datastream, OutputStream outStream, HttpServletResponse response ) {
        this.streamJP2(simplepid, region, size, rotatation, quality, format, datastream, outStream, response, 1);
    }

    public void streamJP2(String simplepid, String region, String size, String rotation, String quality,
            String format, String datastream, OutputStream outStream, HttpServletResponse response,
            int retryServerError) {
        CloseableHttpClient client = HttpClients.createDefault();

        StringBuilder path = new StringBuilder(applicationPathSettings.getLorisPath());

        path.append(simplepid).append("/" + region).append("/" + size)
                .append("/" + rotation).append("/" + quality + "." + format);

        HttpGet method = new HttpGet(path.toString());

        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                if (response != null ) {
                    response.setHeader("Content-Type", "image/jpeg");
                    response.setHeader("content-disposition", "inline");

                    FileIOUtil.stream(outStream, httpResp);
                }
            } else {
                if ((statusCode == 500 || statusCode == 404) && retryServerError > 0) {
                    this.getMetadata(simplepid, datastream, outStream, response, retryServerError - 1);
                } else {
                    LOG.error("Unexpected failure: " + httpResp.getStatusLine().toString());
                    LOG.error("Path was: " + method.getURI());
                }
            }
        } catch (ClientAbortException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User client aborted request to stream jp2 for " + simplepid, e);
            }
        } catch (Exception e) {
            LOG.error("Problem retrieving metadata for " + path, e);
        } finally {
            method.releaseConnection();
        }
    }

    public ApplicationPathSettings getApplicationPathSettings() {
        return applicationPathSettings;
    }

    public void setApplicationPathSettings(ApplicationPathSettings applicationPathSettings) {
        this.applicationPathSettings = applicationPathSettings;
    }
}
