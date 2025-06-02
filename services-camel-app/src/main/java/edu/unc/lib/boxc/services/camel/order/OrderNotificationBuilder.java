package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.services.camel.util.NotificationUtil;

import java.util.List;

/**
 * Builds the email body for OrderNotificationService
 * @author snluong
 */
public class OrderNotificationBuilder {
    /**
     * Build a String containing info about the results of a MultiParentOrderRequest
     * @param request
     * @param successes list of pids for objects that successfully updated
     * @param errors list of errors for unsuccessful order operations
     */
    public String construct(MultiParentOrderRequest request, List<PID> successes, List<String> errors) {
        var parentCount = request.getParentToOrdered().keySet().size();
        var emailBody = "Here are the results of your bulk SetOrderUpdate request.\n";

        emailBody += "Number of parent objects requested: " + parentCount + "\n";
        emailBody += NotificationUtil.getNotificationBody(successes, errors);

        return emailBody;
    }
}
