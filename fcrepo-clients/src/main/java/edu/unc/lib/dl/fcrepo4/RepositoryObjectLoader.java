/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * Provides accessors to retrieve repository objects from a cache
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectLoader {

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

    public void setRepositoryObjectCacheLoader(RepositoryObjectCacheLoader cacheLoader) {
        repositoryObjectCacheLoader = cacheLoader;
    }

    public void setCacheMaxSize(long maxSize) {
        cacheMaxSize = maxSize;
    }

    public void setCacheTimeToLive(long timeToLive) {
        cacheTimeToLive = timeToLive;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
    }

    public AdminUnit getAdminUnit(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof AdminUnit)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not an admin unit");
        }
        return (AdminUnit) repoObj;
    }

    public CollectionObject getCollectionObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof CollectionObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a collection");
        }
        return (CollectionObject) repoObj;
    }

    public ContentRootObject getContentRootObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof ContentRootObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not the content root");
        }
        return (ContentRootObject) repoObj;
    }

    public FolderObject getFolderObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FolderObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a folder");
        }
        return (FolderObject) repoObj;
    }

    public WorkObject getWorkObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof WorkObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a work");
        }
        return (WorkObject) repoObj;
    }

    public FileObject getFileObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FileObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a file");
        }
        return (FileObject) repoObj;
    }

    public BinaryObject getBinaryObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof BinaryObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a binary");
        }
        return (BinaryObject) repoObj;
    }

    public PremisEventObject getPremisEventObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof PremisEventObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a premis event");
        }
        return (PremisEventObject) repoObj;
    }

    public DepositRecord getDepositRecord(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof DepositRecord)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a deposit record");
        }
        return (DepositRecord) repoObj;
    }

    public RepositoryObject getRepositoryObject(PID pid) {
        try {
            return repositoryObjCache.get(pid);
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw (FedoraException) e.getCause();
        }
    }

}
