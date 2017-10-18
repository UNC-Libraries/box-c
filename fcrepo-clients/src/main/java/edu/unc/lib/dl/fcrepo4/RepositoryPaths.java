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
package edu.unc.lib.dl.fcrepo4;

import java.net.URI;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Provides access to base URIs needed for interacting with the repository
 *
 * @author harring
 *
 */
public class RepositoryPaths {

    private static String policiesBase;
    private static String vocabulariesBase;
    private static String depositRecordBase;
    private static String contentBase;
    private static String agentsBase;
    private static String serverUri;
    private static String baseUri;
    private static String baseHost;
    private static PID contentRootPid;

    static {
        setContentBase(System.getProperty("fcrepo.baseUri"));
    }

    private RepositoryPaths() {

    }

    protected static URI getMetadataUri(PID pid) {
        String path = pid.getRepositoryPath();
        if (!path.endsWith(RepositoryPathConstants.FCR_METADATA)) {
            return URI.create(URIUtil.join(path,
                    RepositoryPathConstants.FCR_METADATA));
        } else {
            return pid.getRepositoryUri();
        }
    }

    public static String getServerUri() {
        return serverUri;
    }

    public static String getBaseUri() {
        return baseUri;
    }

    public static String getBaseHost() {
        return baseHost;
    }

    public static String getContentBase() {
        return contentBase;
    }

    public static PID getContentRootPid() {
        return contentRootPid;
    }

    public static String getDepositRecordBase() {
        return depositRecordBase;
    }

    public static String getAgentsBase() {
        return agentsBase;
    }

    public static String getPoliciesBase() {
        return policiesBase;
    }

    public static String getVocabulariesBase() {
        return vocabulariesBase;
    }

    private static void setContentBase(String uri) {
        baseUri = uri;
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        contentBase = URIUtil.join(baseUri, RepositoryPathConstants.CONTENT_BASE);
        contentRootPid = PIDs.get(URIUtil.join(contentBase, RepositoryPathConstants.CONTENT_ROOT_ID));
    }
}
