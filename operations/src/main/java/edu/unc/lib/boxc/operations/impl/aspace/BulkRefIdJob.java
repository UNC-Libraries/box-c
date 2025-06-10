package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Job which runs through a list of PIDs and assigns the corresponding ArchivesSpace ref IDs
 *
 * @author snluong
 */
public class BulkRefIdJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdJob.class);
    private BulkRefIdRequest request;
    private RefIdService service;
    private List<PID> successes;
    private List<String> errors;


    @Override
    public void run() {
        var items  = request.getRefIdMap();
        successes = new ArrayList<>();
        errors = new ArrayList<>();
        items.forEach((pidString, refId) -> {
            var refIdRequest = new RefIdRequest();
            refIdRequest.setPidString(pidString);
            refIdRequest.setAgent(request.getAgent());
            refIdRequest.setRefId(refId);
            try {
                service.updateRefId(refIdRequest);
                successes.add(PIDs.get(pidString));
            } catch (AccessRestrictionException e) {
                log.error("No permission to update ref ID for work {} with error {}", pidString, e);
                errors.add("No permission to update ref ID for work " + pidString + " with error: " + e.getMessage());
            } catch (InvalidOperationForObjectType e) {
                log.error("Unable to update ref ID for object {} with error {}", pidString, e);
                errors.add("Unable to update ref ID for work " + pidString + " with error: " + e.getMessage());
            }
        });
    }

    public void setRequest(BulkRefIdRequest request) {
        this.request = request;
    }

    public void setService(RefIdService service) {
        this.service = service;
    }

    public List<PID> getSuccesses() {
        return successes;
    }

    public List<String> getErrors() {
        return errors;
    }
}
