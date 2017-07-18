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
package edu.unc.lib.dl.fedora;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.httpclient.ConnectionInterruptedHttpMethodRetryHandler;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * Retrieves the access control information for objects stored in Fedora.
 *
 * @author bbpennel
 *
 */
public class FedoraAccessControlService implements AccessControlService {
    private static final Logger log = LoggerFactory.getLogger(FedoraAccessControlService.class);

    private HttpClientConnectionManager httpConnectionManager;
    private CloseableHttpClient httpClient;
    private ObjectMapper mapper;
    private String aclEndpointUrl;
    private String username;
    private String password;

    public FedoraAccessControlService() {
        httpConnectionManager = new PoolingHttpClientConnectionManager();
        this.mapper = new ObjectMapper();
    }

    public void init() {
        httpClient = HttpClientUtil.getAuthenticatedClientBuilder(null, username, password)
                .setConnectionManager(httpConnectionManager)
                .setRetryHandler(new ConnectionInterruptedHttpMethodRetryHandler(10, 3000L))
                .build();
    }

    public void destroy() {
        this.httpClient = null;
        this.mapper = null;
        if (this.httpConnectionManager != null) {
            this.httpConnectionManager.shutdown();
        }
    }

    /**
     * @Inheritdoc
     *
     *             Retrieves the access control from a Fedora JSON endpoint, represented by role to group relations.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ObjectAccessControlsBean getObjectAccessControls(PID pid) {
        HttpGet method = new HttpGet(this.aclEndpointUrl + pid.getPid() + "/getAccess");

        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                Map<?, ?> result = (Map<?, ?>) mapper.readValue(httpResp.getEntity().getContent(), Object.class);
                Map<String, List<String>> roles = (Map<String, List<String>>) result.get("roles");
                Map<String, List<String>> globalRoles = (Map<String, List<String>>) result.get("globals");
                List<String> embargoes = (List<String>) result.get("embargoes");
                List<String> publicationStatus = (List<String>) result.get("publicationStatus");
                List<String> objectState = (List<String>) result.get("objectState");

                return new ObjectAccessControlsBeanImpl(pid, roles, globalRoles, embargoes, publicationStatus,
                        objectState);
            }
        } catch (IOException e) {
            log.error("Failed to retrieve object access control for " + pid, e);
        }

        return null;
    }

    public boolean hasAccess(PID pid, AccessGroupSet groups, Permission permission) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("groups", groups.joinAccessGroups(";")));

        StringBuilder url = new StringBuilder();
        url.append(this.aclEndpointUrl).append(pid.getPid())
            .append("/hasAccess/").append(permission.name())
            .append('?').append(URLEncodedUtils.format(params, "UTF-8"));

        HttpGet method = new HttpGet(url.toString());
        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                String response = EntityUtils.toString(httpResp.getEntity(), "UTF-8");
                Boolean hasAccess = Boolean.parseBoolean(response);
                return hasAccess != null && hasAccess;
            }
        } catch (IOException e) {
            log.error("Failed to check hasAccess for " + pid, e);
        }

        return false;
    }

    public void setAclEndpointUrl(String aclEndpointUrl) {
        this.aclEndpointUrl = aclEndpointUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void assertHasAccess(String message, PID pid, AccessGroupSet groups, Permission permission)
            throws AccessRestrictionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void assertHasAccess(PID pid, AccessGroupSet principals, Permission permission)
            throws AccessRestrictionException {
        assertHasAccess(null, pid, principals, permission);
    }
}
