package edu.unc.lib.boxc.services.camel.util;

import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.List;

public class NotificationUtil {
    private NotificationUtil() {
    }

    public static String getNotificationBody(List<PID> successes, List<String> errors) {
        var successCount = successes.size();
        var emailBody = "";

        emailBody += "Number successfully updated: " + successCount + "\n";

        StringBuilder emailErrors;
        if (errors.isEmpty()) {
            emailErrors = new StringBuilder("There were no errors.");
        } else {
            emailErrors = new StringBuilder("There were the following errors:\n");
            for (String error : errors) {
                emailErrors.append("-- ").append(error).append("\n");
            }
        }

        return emailBody + emailErrors;
    }
}
