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
package edu.unc.lib.dl.ingest.aip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * This filter logs the earlier assignment of a unique identifier to each object being ingested. This
 * identifier scheme is a UUID that is compatible with both the Fedora PID and a URN.
 *
 * @author count0
 *
 */
public class LogIdentifierAssignmentFilter implements AIPIngestFilter {
    private static final Log log = LogFactory.getLog(LogIdentifierAssignmentFilter.class);

    public LogIdentifierAssignmentFilter() {
    }

    public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
	log.debug("Starting LogIdentifierAssignmentFilter");
	for (PID pid : aip.getPIDs()) {
	    aip.getEventLogger().logEvent(
			    PremisEventLogger.Type.NORMALIZATION,
			    "assigned persistently unique Fedora PID with UUID algorithm: " + pid.getPid(), pid);
	}
	log.debug("Finished LogIdentifierAssignmentFilter");
	return aip;
    }
}
