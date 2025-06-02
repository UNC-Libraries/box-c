package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.services.camel.util.NotificationUtil;

import java.util.List;

public class BulkRefIdNotificationBuilder {

    public String construct(BulkRefIdRequest request, List<PID> successes, List<String> errors) {
        var requestCount = request.getRefIdMap().keySet().size();
        var emailBody = "Here are the results of your BulkRefId update request.\n";

        emailBody += "Number of updates requested: " + requestCount + "\n";
        emailBody += NotificationUtil.getNotificationBody(successes, errors);

        return emailBody;
    }
}
