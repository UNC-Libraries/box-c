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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;

import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.rdf.CdrAcl;

/**
 *
 * @author lfarrell
 *
 */
public class StaffOnlyPermissionJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(StaffOnlyPermissionJob.class);

    public StaffOnlyPermissionJob() {
        super();
    }

    public StaffOnlyPermissionJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Setting staff only permissions for deposit {}", getDepositPID());

        Model model = getWritableModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());
        NodeIterator iterator = getChildIterator(depositBag);

        if (iterator.hasNext()) {
            Resource childResc = (Resource) iterator.next();
            setStaffOnly(childResc, model);
        }
    }

    private void setStaffOnly(Resource resc, Model model) {
        Map<String, String> depositStatus = getDepositStatus();
        String staffOnly = depositStatus.get(DepositField.staffOnly.name());

        if (Boolean.parseBoolean(staffOnly)) {
            model.add(resc, CdrAcl.none, PUBLIC_PRINC);
            model.add(resc, CdrAcl.none, AUTHENTICATED_PRINC);
        }
    }
}
