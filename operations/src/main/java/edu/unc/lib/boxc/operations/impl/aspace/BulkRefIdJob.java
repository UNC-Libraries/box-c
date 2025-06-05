package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkRefIdJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdJob.class);
    private BulkRefIdRequest request;
    private RefIdService service;
    @Override
    public void run() {
        var items  = request.getRefIdMap();
        items.forEach((pidString, refId) -> {
            var refIdRequest = new RefIdRequest();
            refIdRequest.setPidString(pidString);
            refIdRequest.setAgent(request.getAgent());
            refIdRequest.setRefId(refId);
            try {
                service.updateRefId(refIdRequest);
            } catch (AccessRestrictionException e) {
                log.error("No permission to update ref ID for work {} with error {}", pidString, e);
            } catch (InvalidOperationForObjectType e) {
                log.error("Unable to update ref ID for object {} with error {}", pidString, e);
            }
        });
    }

    public void setRequest(BulkRefIdRequest request) {
        this.request = request;
    }

    public void setService(RefIdService service) {
        this.service = service;
    }
}
