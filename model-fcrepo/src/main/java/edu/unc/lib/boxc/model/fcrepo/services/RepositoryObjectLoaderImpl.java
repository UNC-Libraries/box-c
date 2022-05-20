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
package edu.unc.lib.boxc.model.fcrepo.services;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.objects.RepositoryObjectCacheLoader;

/**
 * Provides accessors to retrieve repository objects from a cache
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectLoaderImpl implements RepositoryObjectLoader {

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

    @Override
    public AdminUnit getAdminUnit(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof AdminUnit)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not an admin unit");
        }
        return (AdminUnit) repoObj;
    }

    @Override
    public CollectionObject getCollectionObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof CollectionObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a collection");
        }
        return (CollectionObject) repoObj;
    }

    @Override
    public ContentRootObject getContentRootObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof ContentRootObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not the content root");
        }
        return (ContentRootObject) repoObj;
    }

    @Override
    public FolderObject getFolderObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FolderObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a folder");
        }
        return (FolderObject) repoObj;
    }

    @Override
    public WorkObject getWorkObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof WorkObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a work");
        }
        return (WorkObject) repoObj;
    }

    @Override
    public FileObject getFileObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof FileObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a file");
        }
        return (FileObject) repoObj;
    }

    @Override
    public BinaryObject getBinaryObject(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof BinaryObject)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a binary");
        }
        return (BinaryObject) repoObj;
    }

    @Override
    public DepositRecord getDepositRecord(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof DepositRecord)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a deposit record");
        }
        return (DepositRecord) repoObj;
    }

    @Override
    public Tombstone getTombstone(PID pid) {
        RepositoryObject repoObj = getRepositoryObject(pid);
        if (!(repoObj instanceof Tombstone)) {
            throw new ObjectTypeMismatchException("Object with pid " + pid + " is not a tombstone");
        }
        return (Tombstone) repoObj;
    }

    @Override
    public RepositoryObject getRepositoryObject(PID pid) {
        try {
            return repositoryObjCache.get(pid);
        } catch (UncheckedExecutionException | ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new FedoraException((Exception) cause);
            }
        }
    }

    /**
     * Clear any cache entry for the provided pid
     * @param pid
     */
    @Override
    public void invalidate(PID pid) {
        repositoryObjCache.invalidate(pid);
    }

    public void invalidateAll() {
        repositoryObjCache.invalidateAll();
    }

    public LoadingCache<PID, RepositoryObject> getRepositoryObjectCache() {
        return repositoryObjCache;
    }
}
