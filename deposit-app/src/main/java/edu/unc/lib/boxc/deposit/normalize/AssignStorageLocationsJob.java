package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.UnknownStorageLocationException;

/**
 * Assigns a storage location designator to all ingestable objects except for binaries
 *
 * @author bbpennel
 *
 */
public class AssignStorageLocationsJob extends AbstractDepositJob {

    private static final Set<Resource> TYPES_NEEDING_LOCATION = new HashSet<>(asList(
            Cdr.Folder, Cdr.Work, Cdr.Collection, Cdr.AdminUnit, Cdr.FileObject, Cdr.DepositRecord));

    private List<Statement> locAssignments = new ArrayList<>();

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
        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        String storageId = retrieveStorageLocation();

        assignStorageLocation(depositBag, storageId);

        commit(() -> model.add(locAssignments));
    }

    private void assignStorageLocation(Resource resc, String storageId) {
        locAssignments.add(ResourceFactory.createStatement(resc,
                Cdr.storageLocation, ResourceFactory.createStringLiteral(storageId)));

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
