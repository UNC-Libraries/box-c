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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.ObjectHierarchyException;
import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;

/**
 * Factory for retrieving path information for content objects
 *
 * @author bbpennel
 *
 */
public class ContentPathFactoryImpl implements ContentPathFactory {
    private static final Logger log = getLogger(ContentPathFactoryImpl.class);

    private static int MAX_NESTING = 256;

    private LoadingCache<PID, PID> childToParentCache;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    private FcrepoClient fcrepoClient;

    public void init() {
        childToParentCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(new ChildToParentCacheLoader());
    }

    /**
     * Returns the list of PIDs for content objects which are parents of the provided
     * PID, ordered from the base of the hierarchy to the immediate parent of the PID.
     *
     * @param pid
     * @return
     */
    @Override
    public List<PID> getAncestorPids(PID pid) {
        try {
            if (pid.getComponentPath() == null) {
                return buildPath(pid);
            } else {
                // Get the PID of the parent by removing the component path
                PID parentPid = PIDs.get(pid.getId());
                List<PID> ancestors = buildPath(parentPid);
                ancestors.add(parentPid);
                return ancestors;
            }
        } catch (UncheckedExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Override
    public void invalidate(PID pid) {
        childToParentCache.invalidate(pid);
    }

    private List<PID> buildPath(PID pid) {
        PID currentPid = pid;
        List<PID> result = new ArrayList<>();

        int depth = 0;
        while (!RepositoryPaths.getContentRootPid().equals(currentPid) && ++depth < MAX_NESTING) {
            currentPid = childToParentCache.getUnchecked(currentPid);
            result.add(currentPid);
        }

        if (depth >= MAX_NESTING) {
            throw new ObjectHierarchyException("Encountered at least " + depth + " ancestors for " + pid
                    + ", it is either nested too deeply or in a circular hierarchy.");
        }

        Collections.reverse(result);
        return result;
    }

    public void setCacheTimeToLive(long cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    private class ChildToParentCacheLoader extends CacheLoader<PID, PID> {
        @Override
        public PID load(PID pid) {
            try (FcrepoResponse resp = fcrepoClient.get(pid.getRepositoryUri())
                    .preferMinimal()
                    .perform()) {
                Model model = RDFModelUtil.createModel(resp.getBody());
                Resource resc = model.getResource(pid.getRepositoryPath());
                Statement memberStmt = resc.getProperty(PcdmModels.memberOf);
                if (memberStmt == null) {
                    throw new OrphanedObjectException("Object " + pid + " is not the member of any object");
                }
                PID parentPid = PIDs.get(memberStmt.getResource().getURI());
                log.debug("Loaded child to parent relation to cache for {} => {}", pid.getId(), parentPid.getId());
                return parentPid;
            } catch (FcrepoOperationFailedException e) {
                throw ClientFaultResolver.resolve(e);
            } catch (IOException e) {
                throw new RepositoryException("Failed to deserialize response for " + pid, e);
            }
        }
    }
}
