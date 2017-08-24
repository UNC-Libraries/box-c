package edu.unc.lib.dl.fcrepo4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

public class RepositoryObjectLoader {

    private RepositoryObjectDataLoader dataLoader;

    private LoadingCache<PID, RepositoryObject> repositoryObjCache;
    private RepositoryObjectCacheLoader repositoryObjectCacheLoader;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    public void init() {
        repositoryObjCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(repositoryObjectCacheLoader);
    }

    public RepositoryObject getAdminUnit(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof AdminUnit)) {
            throw new FedoraException("Object with pid " + pid + "is not an admin unit");
        }
        return repoObj;
    }

    public RepositoryObject getCollectionObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof CollectionObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a collection");
        }
        return repoObj;
    }

    public RepositoryObject getContentRootObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof ContentRootObject)) {
            throw new FedoraException("Object with pid " + pid + "is not the content root");
        }
        return repoObj;
    }

    public RepositoryObject getFolderObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FolderObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a folder");
        }
        return repoObj;
    }

    public RepositoryObject getWorkObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof WorkObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a work");
        }
        return repoObj;
    }

    public RepositoryObject getFileObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FileObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a file");
        }
        return repoObj;
    }

    public RepositoryObject getBinaryObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof BinaryObject)) {
            throw new FedoraException("Object with pid " + pid + "is not a binary");
        }
        return repoObj;
    }

    public PremisEventObject getPremisEventObject(PID pid) {
        return new PremisEventObject(pid, this, dataLoader).validateType();
    }

    private RepositoryObject getRepositoryObject(PID pid) {
        try {
            return repositoryObjCache.get(pid);
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw (FedoraException) e.getCause();
        }
    }

}
