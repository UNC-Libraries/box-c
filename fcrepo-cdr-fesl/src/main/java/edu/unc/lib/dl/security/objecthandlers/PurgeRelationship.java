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
package edu.unc.lib.dl.security.objecthandlers;

import java.io.IOException;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.security.xacml.sunxacml.attr.AnyURIAttribute;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;


/**
 * Handles REST API method addRelationship
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PurgeRelationship
        extends AbstractFilter {
    private static final Logger logger =
        LoggerFactory.getLogger(PurgeRelationship.class);

    public PurgeRelationship()
    throws PEPException {
        super();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug(this.getClass().getName() + "/handleRequest!");
        }

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;
        try {
            String[] parts = getPathParts(request);
            resAttr = ResourceAttributes.getResources(parts);

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.PURGE_RELATIONSHIP
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM.getStringAttribute());
            
            // adding predicates to the evaluation context
            String predicate = request.getParameter("predicate");
            if (predicate != null && !"".equals(predicate)) {
            	resAttr.put(new URI(AddRelationship.RESOURCE_RELATIONSHIP_PREDICATE), new AnyURIAttribute(new URI(predicate)));
            } else {
            	logger.error("Could not determine predicate for relationship being purged.");
            }

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.PURGE_RELATIONSHIP.getURI()
                                    .toASCIIString(),
                                    parts[1],
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }

        return req;
    }

}
