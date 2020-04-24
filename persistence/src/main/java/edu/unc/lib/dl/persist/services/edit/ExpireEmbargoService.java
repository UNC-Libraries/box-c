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
package edu.unc.lib.dl.persist.services.edit;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;
import edu.unc.lib.dl.rdf.Premis;
import io.dropwizard.metrics5.Timer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import java.util.Date;

import static edu.unc.lib.dl.rdf.CdrAcl.embargoUntil;

/**
 * Service that manages embargo expiration
 *
 * @author smithjp
 *
 */
public class ExpireEmbargoService {

    private RepositoryObjectLoader repoObjLoader;

    private static final Timer timer = TimerFactory.createTimerForClass(EditTitleService.class);

    public ExpireEmbargoService() {
    }

    public void expireEmbargo(PID pid, PatronAccessDetails accessDetails, AgentPrincipals agent) {
        try (Timer.Context context = timer.time()) {
            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
            Model model = ModelFactory.createDefaultModel().add(repoObj.getModel());
            Resource resc = model.getResource(repoObj.getPid().getRepositoryPath());

            String eventText = null;

            // remove embargo from access controls
            Date accessDetailsEmbargo = accessDetails.getEmbargo();

            boolean expiredEmbargo = false;
            if (accessDetailsEmbargo != null) {
                accessDetails.setEmbargo(null);
                expiredEmbargo = true;
            }
            if (resc.hasProperty(embargoUntil)) {
                resc.removeAll(embargoUntil);
                expiredEmbargo = true;
            }

            if (expiredEmbargo) {
                eventText = "Expired an embargo which ended " + accessDetailsEmbargo.toString();
            } else {
                eventText = "Failed to expire embargo.";
            }

            // Produce the premis event for this embargo
            Resource embargoEvent = repoObj.getPremisLog().buildEvent(Premis.ExpireEmbargo)
                    .addImplementorAgent(agent.getUsernameUri())
                    .addEventDetail(eventText)
                    .writeAndClose();
            // write premis event to log
            writePremisEvents(repoObj, embargoEvent);
        }
    }

    private void writePremisEvents(RepositoryObject repoObj, Resource embargoEvent) {
        try (PremisLogger logger = repoObj.getPremisLog()) {
            if (embargoEvent != null) {
                logger.writeEvents(embargoEvent);
            }
        }
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}
