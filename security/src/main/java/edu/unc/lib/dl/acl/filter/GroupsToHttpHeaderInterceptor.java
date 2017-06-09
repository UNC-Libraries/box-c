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
package edu.unc.lib.dl.acl.filter;

import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpComponentsConnection;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * 
 * Interceptor which forwards groups via custom http header in a post request
 * 
 * @author bbpennel
 *
 */
public class GroupsToHttpHeaderInterceptor implements ClientInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(GroupsToHttpHeaderInterceptor.class);

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        LOG.debug("In handleRequest()");
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        if (groups != null) {
            LOG.debug("GOT GROUPS FROM THREAD");
            TransportContext context = TransportContextHolder.getTransportContext();
            HttpComponentsConnection connection = (HttpComponentsConnection) context.getConnection();
            HttpPost postMethod = connection.getHttpPost();
            postMethod.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER,
                    groups.joinAccessGroups(";", null, false));
            LOG.debug("Added HTTP header to POST: " + HttpClientUtil.FORWARDED_GROUPS_HEADER + " : "
                    + groups.joinAccessGroups(";", null, false));
        } else {
            LOG.debug("NO GROUPS SET ON THREAD");
        }
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception e) throws WebServiceClientException {
    }

}
