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
package edu.unc.lib.deposit.normalize;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;
import edu.unc.lib.dl.persist.services.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.services.storage.UnknownStorageLocationException;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Assigns a storage location designator to all ingestable objects except for binaries
 *
 * @author bbpennel
 *
 */
public class AssignStorageLocationsJob extends AbstractDepositJob {

    @Autowired
    private StorageLocationManager locationManager;

    private static final Set<Resource> TYPES_NEEDING_LOCATION = new HashSet<>(asList(
            Cdr.Folder, Cdr.Work, Cdr.Collection, Cdr.AdminUnit, Cdr.FileObject, Cdr.DepositRecord));

    /**
     *
     */
    public AssignStorageLocationsJob() {
    }

    /**
     * @param uuid
     * @param depositUUID
     */
    public AssignStorageLocationsJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model model = getWritableModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        String storageId = retrieveStorageLocation();

        assignStorageLocation(depositBag, storageId);
    }

    private void assignStorageLocation(Resource resc, String storageId) {
        resc.addLiteral(Cdr.storageLocation, storageId);

        NodeIterator iterator = getChildIterator(resc);
        // No more children, nothing further to do in this tree
        if (iterator == null) {
            return;
        }

        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();
                if (!needsStorageLocation(childResc)) {
                    continue;
                }
                assignStorageLocation(childResc, storageId);
            }
        } finally {
            iterator.close();
        }
    }

    private boolean needsStorageLocation(Resource resc) {
        StmtIterator it = resc.listProperties(RDF.type);
        while (it.hasNext()) {
            Statement stmt = it.next();
            if (TYPES_NEEDING_LOCATION.contains(stmt.getResource())) {
                return true;
            }
        }
        return false;
    }

    private String retrieveStorageLocation() {
        PID destPid = getDestinationPID();

        Map<String, String> depositStatus = getDepositStatus();
        String providedStorageId = depositStatus.get(DepositField.storageLocation.name());

        if (providedStorageId == null) {
            // Handle default storage location case
            StorageLocation loc = locationManager.getStorageLocation(destPid);
            if (loc == null) {
                throw new UnknownStorageLocationException("Unable to determine storage location for destination "
                        + destPid.getId() + " in deposit " + depositUUID);
            }

            return loc.getId();
        } else {
            // Handle client provided storage location
            StorageLocation loc = locationManager.getStorageLocationById(providedStorageId);
            if (loc == null) {
                throw new UnknownStorageLocationException("Unknown location " + providedStorageId
                        + " specified for deposit " + depositUUID);
            }

            // Verify that the select storage location is allowed for the destination container
            List<StorageLocation> available = locationManager.listAvailableStorageLocations(destPid);
            if (!available.contains(loc)) {
                failJob("Illegal storage location for destination",
                        "Storage location " + loc.getId() + " is not valid for destination " + destPid.getId());
            }

            return loc.getId();
        }
    }
}
