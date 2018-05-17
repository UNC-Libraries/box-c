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
package edu.unc.lib.dl.persist.services.destroy;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.Tombstone;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.Premis;
import io.dropwizard.metrics5.Timer;

/**
 *
 * @author harring
 *
 */
public class DestroyObjectsJob implements Runnable {
    private static final Timer timer = TimerFactory.createTimerForClass(DestroyObjectsJob.class);

    private List<PID> objsToDestroy;
    private AgentPrincipals agent;
    private RepositoryObjectFactory repoObjFactory;

    public DestroyObjectsJob(AgentPrincipals agent, List<PID> objsToDestroy) {
        this.objsToDestroy = objsToDestroy;
    }

    @Override
    public void run() {
        try (Timer.Context context = timer.time()) {
            // create tombstone for each destroyed obj
            Tombstone tStone = null;
            for (PID pid : objsToDestroy) {
                Model model = ModelFactory.createDefaultModel();
                tStone = repoObjFactory.createTombstone(pid, model);

                //TODO: remove containment relation from obj's parent

                //add premis event to tombstone
                tStone.getPremisLog().buildEvent(Premis.Deletion)
                .addImplementorAgent(agent.getUsernameUri())
                .addEventDetail("Item deleted from repository and replaced by this tombstone")
                .write();

                tStone.getRecord().put("PREMIS log", tStone.getPremisLog().getEvents().toString());
            }
        }
    }



}
