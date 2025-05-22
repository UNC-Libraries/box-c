package edu.unc.lib.boxc.operations.impl.aspace;

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
            service.updateRefId(refIdRequest);
        });
    }

    public void setRequest(BulkRefIdRequest request) {
        this.request = request;
    }

    public void setService(RefIdService service) {
        this.service = service;
    }
}
