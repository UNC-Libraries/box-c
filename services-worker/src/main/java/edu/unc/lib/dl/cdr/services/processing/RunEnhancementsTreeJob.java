/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.processing;

import java.util.List;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author bbpennel
 * @date Aug 14, 2015
 */
public class RunEnhancementsTreeJob implements Runnable {

    private TripleStoreQueryService tripleStoreQueryService;

    private final List<PID> targets;
    private final boolean force;

    private Client jesqueClient;

    private String runEnhancementTreeQueue;

    public RunEnhancementsTreeJob(List<String> targets, boolean force) {
        this.targets = PID.toPIDList(targets);
        this.force = force;
    }

    @Override
    public void run() {
        for (PID target : targets) {
            List<PID> allChildren = tripleStoreQueryService.fetchAllContents(target);
            allChildren.add(target);

            for (PID pid : allChildren) {
                jesqueClient.enqueue(runEnhancementTreeQueue,
                        new Job(ApplyEnhancementServicesJob.class.getName(), pid.getPid(), force));
            }
        }
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
        this.tripleStoreQueryService = tripleStoreQueryService;
    }

    public void setJesqueClient(Client jesqueClient) {
        this.jesqueClient = jesqueClient;
    }

    public void setRunEnhancementTreeQueue(String runEnhancementTreeQueue) {
        this.runEnhancementTreeQueue = runEnhancementTreeQueue;
    }

}
