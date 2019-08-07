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
package edu.unc.lib.dl.pidgen;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;

/**
 * PIDGenerator that uses a remote Fedora instance to generate PIDs.
 *
 * http://localhost:8080/fedora/management/getNextPID?numPIDs=10&namespace=nara&xml=true
 *
 * @author count0
 *
 */
public class RemotePIDGenerator implements PIDGenerator {
    private ManagementClient managementClient;

    private static final Logger logger = LoggerFactory.getLogger(RemotePIDGenerator.class.getName());

    private String namespace;

    public RemotePIDGenerator() {
    }

    @Override
    public PID getNextPID() {
        return getNextPIDs(1).get(0);
    }

    @Override
    public List<PID> getNextPIDs(int howMany) {
        if (logger.isDebugEnabled()) {
            if (namespace != null) {
                logger.debug("Using namespace '" + namespace + "'");
            } else {
                logger.debug("No namespace provided; will use default of server");
            }
        }
        try {
            return managementClient.getNextPID(howMany, namespace);
        } catch (FedoraException e) {
            throw new Error("Cannot retrieve PID from Fedora service", e);
        }
    }

    public ManagementClient getManagementClient() {
        return managementClient;
    }

    public void setManagementClient(ManagementClient managementClient) {
        this.managementClient = managementClient;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
