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
package edu.unc.lib.dl.services.camel.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants;

/**
 * Processor which invalidates cache entries for updated objects
 *
 * @author bbpennel
 */
public class CacheInvalidatingProcessor implements Processor {
    private static final Logger log = getLogger(CacheInvalidatingProcessor.class);
    private RepositoryObjectLoader repoObjLoader;
    private ObjectAclFactory objectAclFactory;

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String fcrepoUri = (String) in.getHeader(FcrepoHeaders.FCREPO_URI);
        if (fcrepoUri == null) {
            String fcrepoId = (String) in.getHeader(FcrepoJmsConstants.IDENTIFIER);
            String fcrepoBaseUrl = (String) in.getHeader(FcrepoJmsConstants.BASE_URL);
            fcrepoUri = fcrepoBaseUrl + fcrepoId;
        }

        PID pid;
        try {
            pid = PIDs.get(fcrepoUri);
        } catch (Exception e) {
            log.debug("Failed to parse fcrepo id {} as PID while filtering: {}", fcrepoUri, e.getMessage());
            return;
        }
        // Filter out non-content objects
        if (pid == null || !PIDConstants.CONTENT_QUALIFIER.equals(pid.getQualifier())) {
            return;
        }
        log.debug("Invalidating caches for {}", pid);
        repoObjLoader.invalidate(pid);
        objectAclFactory.invalidate(pid);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setObjectAclFactory(ObjectAclFactory objectAclFactory) {
        this.objectAclFactory = objectAclFactory;
    }
}
