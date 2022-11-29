package edu.unc.lib.boxc.operations.impl.events;

import java.io.File;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * A factory class for creating PremisLogger instances
 *
 * @author harring
 */

public class PremisLoggerFactoryImpl implements PremisLoggerFactory {

    private PIDMinter pidMinter;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;
    private BinaryTransferService transferService;

    /**
     * Create a PREMIS logger for events related to the object identified by pid. Events
     * will be stored to/retrieved from the provided local file.
     *
     * @param pid pid of the subject of the logger
     * @param file file where the event data is stored
     * @return new PremisLogger instance
     */
    @Override
    public PremisLogger createPremisLogger(PID pid, File file) {
        return new FilePremisLogger(pid, file, pidMinter);
    }

    /**
     * Create a premis logger for events related to the provided repository object.
     *
     * @param repoObject subject of the logger
     * @return new PremisLogger instance
     */
    @Override
    public PremisLogger createPremisLogger(RepositoryObject repoObject) {
        return createPremisLogger(repoObject, transferService.getSession(repoObject));
    }

    /**
     * Create a PREMIS logger for events related to the provided repository object.
     *
     * @param repoObject subject of the logger
     * @param session session the logger will use for transferring log data to storage
     * @return new PremisLogger instance
     */
    @Override
    public PremisLogger createPremisLogger(RepositoryObject repoObject, BinaryTransferSession session) {
        return new RepositoryPremisLogger(repoObject, session, pidMinter, repoObjLoader, repoObjFactory);
    }

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(PIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    /**
     * @param repoObjLoader the repoObjLoader to set
     */
    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param repoObjFactory the repoObjFactory to set
     */
    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setBinaryTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }
}
