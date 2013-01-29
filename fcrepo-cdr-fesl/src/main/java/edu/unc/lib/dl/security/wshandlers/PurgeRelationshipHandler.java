/*
 * File: PurgeRelationshipHandler.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.unc.lib.dl.security.wshandlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.pep.ws.operations.AbstractOperationHandler;
import org.fcrepo.server.security.xacml.pep.ws.operations.OperationHandlerException;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.RequestCtx;


/**
 * @author nishen@melcoe.mq.edu.au
 */
public class PurgeRelationshipHandler
        extends AbstractOperationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PurgeRelationshipHandler.class);
    
	public static final String RESOURCE_RELATIONSHIP_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate";

    public PurgeRelationshipHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        return null;
    }

    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("PurgeRelationshipHandler/handleRequest!");

        RequestCtx req = null;
        Object oMap = null;

        String pid = null;

        try {
            oMap = getSOAPRequestObjects(context);
            logger.debug("Retrieved SOAP Request Objects");
        } catch (SoapFault af) {
            logger.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }

        try {
            pid = (String) callGetter("getPid",oMap);
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        logger.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        try {
            resAttr = ResourceAttributes.getResources(pid);

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.PURGE_RELATIONSHIP
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM
                                .getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));
            
            addPredicate(context, resAttr);

            LogUtil.statLog(getUser(context),
                            Constants.ACTION.PURGE_RELATIONSHIP.getURI()
                                    .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }
    
    protected void addPredicate(SOAPMessageContext context, Map<URI, AttributeValue> resAttr) throws OperationHandlerException, URISyntaxException {
        Object oMap = null;
        String relationship = null;
        try {
            oMap = getSOAPRequestObjects(context);
            logger.debug("Retrieved SOAP Request Objects");
        } catch (SoapFault af) {
            logger.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }
        try {
        	relationship = (String) callGetter("getRelationship",oMap);
            if (relationship != null && !"".equals(relationship)) {
            	resAttr.put(new URI(RESOURCE_RELATIONSHIP_PREDICATE), new AnyURIAttribute(new URI(relationship)));
            }
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }
        logger.debug("Extracted SOAP Request Objects");
    }
}
