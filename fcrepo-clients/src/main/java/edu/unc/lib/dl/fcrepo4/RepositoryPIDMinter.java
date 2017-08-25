package edu.unc.lib.dl.fcrepo4;

import java.util.UUID;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

public class RepositoryPIDMinter {

    /**
     * Mint a PID for a new deposit record object
     *
     * @return PID in the deposit record path
     */
    public PID mintDepositRecordPid() {
        String uuid = UUID.randomUUID().toString();
        String id = URIUtil.join(RepositoryPathConstants.DEPOSIT_RECORD_BASE, uuid);

        return PIDs.get(id);
    }

    /**
     * Mint a PID for a new content object
     *
     * @return PID in the content path
     */
    public PID mintContentPid() {
        String uuid = UUID.randomUUID().toString();
        String id = URIUtil.join(RepositoryPathConstants.CONTENT_BASE, uuid);

        return PIDs.get(id);
    }

    /**
     * Mints a URL for a new event object belonging to the provided parent object
     *
     * @param parentPid The object which this event will belong to.
     * @return
     */
    public PID mintPremisEventPid(PID parentPid) {
        String uuid = UUID.randomUUID().toString();
        String eventUrl = URIUtil.join(parentPid.getRepositoryPath(),
                RepositoryPathConstants.EVENTS_CONTAINER, uuid);
        return PIDs.get(eventUrl);
    }

}
