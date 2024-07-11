package edu.unc.lib.boxc.model.fcrepo.event;

import java.util.concurrent.locks.Lock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;

/**
 * A PREMIS log for a repository object
 * @author bbpennel
 */
public class RepositoryPremisLog implements PremisLog {
    protected static final PidLockManager lockManager = PidLockManager.getDefaultPidLockManager();
    protected RepositoryObject repoObject;
    protected RepositoryObjectLoader repoObjLoader;

    public RepositoryPremisLog(RepositoryObject repoObject, RepositoryObjectLoader repoObjLoader) {
        this.repoObject = repoObject;
        this.repoObjLoader = repoObjLoader;
    }

    @Override
    public Model getEventsModel() {
        PID logPid = DatastreamPids.getMdEventsPid(repoObject.getPid());
        Lock logLock = lockManager.awaitReadLock(logPid);
        try {
            BinaryObject eventsObj = repoObjLoader.getBinaryObject(logPid);
            return RDFModelUtil.createModel(eventsObj.getBinaryStream(), "N-TRIPLE");
        } catch (NotFoundException e) {
            return ModelFactory.createDefaultModel();
        } finally {
            logLock.unlock();
        }
    }
}
