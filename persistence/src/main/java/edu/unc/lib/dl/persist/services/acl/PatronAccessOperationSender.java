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
package edu.unc.lib.dl.persist.services.acl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.dl.services.MessageSender;

/**
 * Service which sends requests to update patron access
 *
 * @author bbpennel
 */
public class PatronAccessOperationSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(PatronAccessOperationSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(PatronAccessAssignmentRequest.class);

    /**
     * Push a patron access update job
     *
     * @param request
     * @throws IOException
     */
    public void sendUpdateRequest(PatronAccessAssignmentRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update patron access for {} has been queued for {}",
                request.getTargetPid(), request.getAgent().getUsername());
    }
}
