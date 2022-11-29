package edu.unc.lib.boxc.model.api.ids;

/**
 * @author bbpennel
 */
public interface PIDMinter {

    /**
     * Mint a PID for a new deposit record object
     *
     * @return PID in the deposit record path
     */
    PID mintDepositRecordPid();

    /**
     * Mint a PID for a new content object
     *
     * @return PID in the content path
     */
    PID mintContentPid();

    /**
     * Mints a URL for a new event object belonging to the provided parent object
     *
     * @param parentPid The object which this event will belong to.
     * @return
     */
    PID mintPremisEventPid(PID parentPid);

}