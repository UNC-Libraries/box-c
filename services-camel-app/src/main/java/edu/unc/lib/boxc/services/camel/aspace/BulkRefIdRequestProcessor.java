package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.operations.impl.aspace.BulkRefIdJob;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for Bulk Ref ID update requests
 *
 * @author snluong
 */
public class BulkRefIdRequestProcessor implements Processor {
    private RefIdService refIdService;
    private BulkRefIdNotificationService bulkRefIdNotificationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        var in = exchange.getIn();
        var request = BulkRefIdRequestSerializationHelper.toRequest(in.getBody(String.class));
        var job = new BulkRefIdJob();
        job.setRequest(request);
        job.setService(refIdService);
        job.run();

        bulkRefIdNotificationService.sendResults(request, job.getSuccesses(), job.getErrors());
    }

    public void setRefIdService(RefIdService refIdService) {
        this.refIdService = refIdService;
    }

    public void setBulkRefIdNotificationService(BulkRefIdNotificationService bulkRefIdNotificationService) {
        this.bulkRefIdNotificationService = bulkRefIdNotificationService;
    }
}
