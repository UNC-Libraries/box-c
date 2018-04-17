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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.REPOSITORY_ROOT_ID;

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
    private static PID contentBasePid;
    private static PID rootPid;

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

    /**
     * Get a string of the uri identifying the base of the repository.
     *
     * @return
     */
    public static String getBaseUri() {
        return baseUri;
    }

    public static String getBaseHost() {
        return baseHost;
    }

    /**
     * @return the rootPid
     */
    public static PID getRootPid() {
        return rootPid;
    }

    /**
     * @return pid for the resource where content objects are stored.
     */
    public static PID getContentBasePid() {
        return contentBasePid;
    }

    /**
     * @return base uri for content objects
     */
    public static String getContentBase() {
        return contentBase;
    }

    /**
     * @return PID of the root object of the content tree.
     */
    public static PID getContentRootPid() {
        return contentRootPid;
    }

    /**
     * @return base uri for deposit record objects
     */
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

    /**
     * Prepend id with defined levels of hashed containers based on the values.
     * For example, 9bd8b60e-93a2-4b66-8f0a-b62338483b39 would become
     *    9b/d8/b6/9bd8b60e-93a2-4b66-8f0a-b62338483b39
     *
     * @param id
     * @return
     */
    public static String idToPath(String id, int pathDepth, int length) {
        StringBuilder sb = new StringBuilder();

        // Expand the id into chunked subfolders
        for (int i = 0; i < pathDepth; i++) {
            sb.append(id.substring(i * length, i * length + length))
                    .append('/');
        }

        return sb.toString();
    }

    /**
     * Initializes paths from repository base uri
     *
     * @param uri base uri for repository
     */
    private static void setContentBase(String uri) {
        baseUri = uri;
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        rootPid = new FedoraPID(REPOSITORY_ROOT_ID, REPOSITORY_ROOT_ID, null, URI.create(baseUri));
        contentBase = URIUtil.join(baseUri, CONTENT_BASE);
        contentBasePid = new FedoraPID(CONTENT_BASE, REPOSITORY_ROOT_ID, null, URI.create(contentBase));
        contentRootPid = PIDs.get(URIUtil.join(contentBase, CONTENT_ROOT_ID));
    }
}
