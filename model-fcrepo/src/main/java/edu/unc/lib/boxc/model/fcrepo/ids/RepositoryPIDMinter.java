package edu.unc.lib.boxc.model.fcrepo.ids;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.EVENT_ID_PREFIX;

import java.util.UUID;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;

/**
 * Minter of PIDs for content objects, deposit records, and PREMIS events.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryPIDMinter implements PIDMinter {

    /**
     * Mint a PID for a new deposit record object
     *
     * @return PID in the deposit record path
     */
    @Override
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
    @Override
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
    @Override
    public PID mintPremisEventPid(PID parentPid) {
        String eventId = EVENT_ID_PREFIX + System.currentTimeMillis() + System.nanoTime();
        String eventUrl = URIUtil.join(parentPid.getRepositoryPath(), eventId);
        return PIDs.get(eventUrl);
    }

}
