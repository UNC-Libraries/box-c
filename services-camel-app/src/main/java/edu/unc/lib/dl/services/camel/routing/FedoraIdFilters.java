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
package edu.unc.lib.dl.services.camel.routing;

import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants;
import edu.unc.lib.dl.services.camel.util.EventTypes;

/**
 * Filters for Fedora message routing
 *
 * @author bbpennel
 */
public class FedoraIdFilters {
    private static final Logger log = getLogger(FedoraIdFilters.class);

    private FedoraIdFilters() {
    }

    /**
     * Filter Fedora messages to exclude any messages that should not be considered for triples indexing
     *
     * @param exchange
     * @return
     */
    public static boolean allowedForTripleIndex(Exchange exchange) {
        Message in = exchange.getIn();
        String fcrepoId = (String) in.getHeader(FcrepoJmsConstants.IDENTIFIER);
        // Exclude fcr:versions, audit path
        return !(fcrepoId.contains(RepositoryPathConstants.FCR_VERSIONS)
                || fcrepoId.endsWith("/audit")
                || fcrepoId.endsWith(RepositoryPathConstants.FCR_TOMBSTONE)
                || fcrepoId.endsWith("/fedora:timemap"));
    }

    /**
     * Filter Fedora messages to exclude any messages that should not be considered for longleaf registration
     *
     * @param exchange
     * @return
     */
    public static boolean allowedForLongleaf(Exchange exchange) {
        Message in = exchange.getIn();
        String resourceType = (String) in.getHeader(FcrepoJmsConstants.RESOURCE_TYPE);
        String fcrepoUri = (String) exchange.getIn().getHeader(FCREPO_URI);
        if (!resourceType.contains(Binary.getURI()) || fcrepoUri.endsWith(RepositoryPathConstants.FCR_METADATA)) {
            return false;
        }
        String eventType = (String) in.getHeader(FcrepoJmsConstants.EVENT_TYPE);
        return eventType != null && (eventType.contains(EventTypes.EVENT_CREATE)
                || eventType.contains(EventTypes.EVENT_UPDATE));
    }

    private static final Pattern IGNORE_SUFFIX = Pattern.compile(".*(fcr|fedora):.+");

    /**
     * Filter Fedora messages to exclude any messages that should not undergoing enhancement processing
     *
     * @param exchange
     * @return
     */
    public static boolean allowedForEnhancements(Exchange exchange) {
        Message in = exchange.getIn();
        String eventType = (String) in.getHeader(FcrepoJmsConstants.EVENT_TYPE);
        if (eventType == null || !eventType.contains(EventTypes.EVENT_CREATE)) {
            return false;
        }

        String fcrepoId = (String) in.getHeader(FcrepoJmsConstants.IDENTIFIER);
        String fcrepoBaseUrl = (String) in.getHeader(FcrepoJmsConstants.BASE_URL);
        PID pid;
        try {
            pid = PIDs.get(fcrepoBaseUrl + fcrepoId);
        } catch (Exception e) {
            log.debug("Failed to parse fcrepo id {} as PID while filtering: {}", fcrepoId, e.getMessage());
            return false;
        }
        if (pid == null) {
            log.error("Received null PID for object with fcrepo URI {}", fcrepoBaseUrl + fcrepoId);
            return false;
        }
        // Filter out non-content objects
        if (!PIDConstants.CONTENT_QUALIFIER.equals(pid.getQualifier())) {
            return false;
        }
        // Allow for content root object to be indexed
        if (RepositoryPathConstants.CONTENT_ROOT_ID.equals(pid.getId())) {
            return true;
        }
        String componentPath = pid.getComponentPath();
        // No component path, is a full content object
        if (componentPath == null) {
            return true;
        }
        // PID is that of an intermediate container resource
        int slashIndex = componentPath.indexOf('/');
        if (slashIndex == -1 || slashIndex == componentPath.length() - 1) {
            return false;
        }
        String afterContainer = componentPath.substring(slashIndex + 1);
        if (IGNORE_SUFFIX.matcher(afterContainer).matches()) {
            return false;
        }

        return DatastreamType.ORIGINAL_FILE.getId().equals(afterContainer);
    }
}
