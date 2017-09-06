package edu.unc.lib.dl.fcrepo4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
/**
 *
 * @author bbpennel
 * @author harring
 *
 */

public class RepositoryObjectLoader {

    private RepositoryObjectDataLoader dataLoader;

    private LoadingCache<PID, RepositoryObject> repositoryObjCache;
    private RepositoryObjectCacheLoader repositoryObjectCacheLoader;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    private RepositoryObjectFactory repoObjFactory;

    public void init() {
        repositoryObjCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(repositoryObjectCacheLoader);
    }

    public void setRepositoryObjectCacheLoader(RepositoryObjectCacheLoader cacheLoader) {
        repositoryObjectCacheLoader = cacheLoader;
    }

    public void setCacheMaxSize(long maxSize) {
        cacheMaxSize = maxSize;
    }

    public void setCacheTimeToLive(long timeToLive) {
        cacheTimeToLive = timeToLive;
    }

    public AdminUnit getAdminUnit(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof AdminUnit)) {
            throw new FedoraException("Object with pid " + pid + "is not an admin unit");
        }
        return (AdminUnit) repoObj;
    }

    public CollectionObject getCollectionObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof CollectionObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a collection");
        }
        return (CollectionObject) repoObj;
    }

    public ContentRootObject getContentRootObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof ContentRootObject)) {
            throw new FedoraException("Object with pid " + pid + "is not the content root");
        }
        return (ContentRootObject) repoObj;
    }

    public FolderObject getFolderObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FolderObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a folder");
        }
        return (FolderObject) repoObj;
    }

    public WorkObject getWorkObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof WorkObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a work");
        }
        return (WorkObject) repoObj;
    }

    public FileObject getFileObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FileObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a file");
        }
        return (FileObject) repoObj;
    }

    public BinaryObject getBinaryObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof BinaryObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a binary");
        }
        return (BinaryObject) repoObj;
    }

    public PremisEventObject getPremisEventObject(PID pid) {
        return new PremisEventObject(pid, this, dataLoader, repoObjFactory).validateType();
    }

    public DepositRecord getDepositRecord(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof DepositRecord)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + "is not a deposit record");
        }
        return (DepositRecord) repoObj;
    }

    protected RepositoryObject getRepositoryObject(PID pid) {
        try {
            return repositoryObjCache.get(pid);
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw (FedoraException) e.getCause();
        }
    }

}
