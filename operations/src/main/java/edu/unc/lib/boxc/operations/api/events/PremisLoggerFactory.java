package edu.unc.lib.boxc.operations.api.events;

import java.io.File;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Interface for a factory for creating PremisLogger instances
 * @author bbpennel
 */
public interface PremisLoggerFactory {

    /**
     * Create a PREMIS logger for events related to the object identified by pid. Events
     * will be stored to/retrieved from the provided local file.
     *
     * @param pid pid of the subject of the logger
     * @param file file where the event data is stored
     * @return new PremisLogger instance
     */
    PremisLogger createPremisLogger(PID pid, File file);

    /**
     * Create a premis logger for events related to the provided repository object.
     *
     * @param repoObject subject of the logger
     * @return new PremisLogger instance
     */
    PremisLogger createPremisLogger(RepositoryObject repoObject);

    /**
     * Create a PREMIS logger for events related to the provided repository object.
     *
     * @param repoObject subject of the logger
     * @param session session the logger will use for transferring log data to storage
     * @return new PremisLogger instance
     */
    PremisLogger createPremisLogger(RepositoryObject repoObject, BinaryTransferSession session);

}