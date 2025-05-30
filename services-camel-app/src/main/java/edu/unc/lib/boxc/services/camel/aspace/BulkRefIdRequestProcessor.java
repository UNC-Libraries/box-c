package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.operations.impl.aspace.BulkRefIdJob;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkRefIdRequestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdRequestProcessor.class);
    private RefIdService refIdService;
    private BulkRefIdJob job;

    @Override
    public void process(Exchange exchange) throws Exception {
        var in = exchange.getIn();
        var request = BulkRefIdRequestSerializationHelper.toRequest(in.getBody(String.class));

        job.setRequest(request);
        job.setService(refIdService);
        job.run();
    }

    public void setRefIdService(RefIdService refIdService) {
        this.refIdService = refIdService;
    }

    public void setJob(BulkRefIdJob job) {
        this.job = job;
    }
}
