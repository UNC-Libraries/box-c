package edu.unc.lib.boxc.auth.api.services;

import java.util.Date;

/**
 * Helper methods for interacting with embargoes
 *
 * @author bbpennel
 *
 */
public class EmbargoUtil {

    private EmbargoUtil() {
    }

    /**
     * Returns true if the provided embargo end date is active based
     * on the current date.
     *
     * @param embargoUntil
     * @return true if the embargo is active.
     */
    public static boolean isEmbargoActive(Date embargoUntil) {
        if (embargoUntil != null) {
            Date currentDate = new Date();
            if (currentDate.before(embargoUntil)) {
                return true;
            }
        }
        return false;
    }
}
