package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;

import java.util.List;

public class BulkRefIdNotificationService {
    private EmailHandler emailHandler;
    private BulkRefIdNotificationBuilder builder;

    public void sendResults(BulkRefIdRequest request, List<PID> successes, List<String> errors) {
        if (request.getEmail() == null) {
            return;
        }

        var emailBody = builder.construct(request, successes, errors);
        emailHandler.sendEmail(request.getEmail(), "DCR Bulk Ref ID update completed", emailBody, null, null);
    }

    public void setEmailHandler(EmailHandler emailHandler) {
        this.emailHandler = emailHandler;
    }

    public void setBuilder(BulkRefIdNotificationBuilder builder) {
        this.builder = builder;
    }
}
