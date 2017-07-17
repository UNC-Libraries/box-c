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

import net.greghaines.jesque.Job;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * 
 * @author count0
 *
 */
public class EnhancementConductor implements MessageConductor {

    private net.greghaines.jesque.client.Client jesqueClient;
    private String queueName;

    public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
        this.jesqueClient = jesqueClient;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void add(ActionMessage actionMessage) {
        EnhancementMessage message = (EnhancementMessage) actionMessage;

        // TODO? proper JSON serialization of enhancement messages so we just pass the message.
        Job job = new Job(ApplyEnhancementServicesJob.class.getName(), message.getPid().getPid(),
                message.getNamespace(), message.getAction(), message.getServiceName(), message.getFilteredServices());
        jesqueClient.enqueue(queueName, job);
    }

    public static final String identifier = "JESQUE_ENHANCEMENT";

    /**
     * Returns the identifier string for this conductor
     * @return
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

}
