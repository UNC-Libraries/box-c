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

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author count0
 *
 */
public class ApplicationPathSettings {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPathSettings.class);

    private String apiRecordPath;
    private String fedoraPath;
    private URL fedoraURL;
    private String lorisPath;
    private String solrPath;
    private URL solrURL;
    private String externalContentPath;

    public String getLorisPath() {
        return lorisPath;
    }
    public void setlorisPath(String lorisPath) {
        this.lorisPath = lorisPath;
    }
    public String getApiRecordPath() {
        return apiRecordPath;
    }
    public void setApiRecordPath(String apiRecordPath) {
        this.apiRecordPath = apiRecordPath;
    }
    public String getFedoraPath() {
        return fedoraPath;
    }
    public void setFedoraPath(String fedoraPath) {
        this.fedoraPath = fedoraPath;
        try {
            this.fedoraURL = new URL(fedoraPath);
        } catch (MalformedURLException e) {
            LOG.error("Failed to set fedora path to " + fedoraPath, e);
        }
    }
    public String getSolrPath() {
        return solrPath;
    }
    public void setSolrPath(String solrPath) {
        this.solrPath = solrPath;
        try {
            this.solrURL = new URL(solrPath);
        } catch (MalformedURLException e) {
            LOG.error("Failed to set solr path to " + solrPath, e);
        }
    }
    public String getExternalContentPath() {
        return externalContentPath;
    }
    public void setExternalContentPath(String externalContentPath) {
        this.externalContentPath = externalContentPath;
    }
    public URL getFedoraURL() {
        return fedoraURL;
    }
    public URL getSolrURL() {
        return solrURL;
    }
    public String getFedoraPathWithoutDefaultPort( ) {
        return getPathWithoutDefaultPort(fedoraURL);
    }
    private String getPathWithoutDefaultPort(URL url ) {
        if (url.getDefaultPort() != url.getPort()) {
            return url.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url.getProtocol()).append("://").append(url.getHost()).append(url.getPath());
        return sb.toString();

    }
}
