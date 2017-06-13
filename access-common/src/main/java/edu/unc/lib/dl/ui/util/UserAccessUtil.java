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
package edu.unc.lib.dl.ui.util;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.InvalidDatastreamException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Utility for determining if a particular user has access to an object or datastream.
 * 
 * @author bbpennel
 */
public class UserAccessUtil {
    private static final Logger LOG = LoggerFactory.getLogger(UserAccessUtil.class);

    private Permission defaultPermission = Permission.viewDescription;
    private AccessControlService accessControlService;
    // Temporarily using this to check publication status
    private SolrQueryLayerService solrQueryLayer;
    // Cache of answers as to whether or not a user has access to a particular object or object's datastream
    // <PID, <group, Answer>>
    private WeakHashMap<String, Map<String, Boolean>> pids2User2Access = new WeakHashMap<String, Map<String, Boolean>>(
            256);

    private AtomicLong lastCleared = new AtomicLong(0);
    // Time interval between cache clears
    private long clearInterval = 1000 * 60 * 2;

    public boolean hasAccess(String id, String user, AccessGroupSet groups) {
        clearCacheOnInterval();

        // Check for cached version
        Map<String, Boolean> user2Access = this.pids2User2Access.get(id);
        if (user2Access != null) {
            Boolean answer = user2Access.get(user);
            if (answer != null) {
                LOG.debug("Answering user " + user + " from cache for " + id + " with answer " + answer);
                return answer;
            }
        }

        PID pid = new PID(id);
        boolean answer;
        if (groups.contains(AccessGroupConstants.ADMIN_GROUP)) {
            answer = true;
        } else {
            // Determine what permission we are looking for
            String[] idParts = pid.getPid().split("/");
            Permission permission = null;
            Datastream datastream = null;
            if (idParts.length > 1) {
                id = idParts[0];
                datastream = Datastream.getDatastream(idParts[1]);
                if (datastream == null) {
                    throw new InvalidDatastreamException(idParts[1] + " is not a valid datastream identifer");
                }
                permission = Permission.getPermissionByDatastreamCategory(datastream.getCategory());
            } else {
                permission = defaultPermission;
            }

            // Check if the item is accessible to the user according to publish status
            SimpleIdRequest isPublishedRequest = new SimpleIdRequest(id, groups);
            boolean isPublished = solrQueryLayer.isAccessible(isPublishedRequest);

            // Get access info from Fedora if user is not blocked by publication rights and this is a datastream request
            if (isPublished && datastream != null) {
                ObjectAccessControlsBean aclBean = accessControlService.getObjectAccessControls(new PID(id));
                answer = aclBean.hasPermission(groups, permission);
            } else {
                answer = isPublished;
            }
        }

        if (user2Access == null) {
            user2Access = new HashMap<String, Boolean>();
            this.pids2User2Access.put(pid.getPid(), user2Access);
        }

        user2Access.put(user, answer);
        LOG.debug("Answering user " + user + " for " + id + " with answer " + answer + ", storing to cache");
        return answer;
    }

    private void clearCacheOnInterval() {
        synchronized (lastCleared) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleared.get() > clearInterval) {
                if (this.pids2User2Access.size() > 0) {
                    this.pids2User2Access.clear();
                }
                lastCleared.set(currentTime);
            }
        }
    }

    public void updateAccess(String pid, String user, AccessGroupSet groups) {
        this.pids2User2Access.remove(pid);
        this.hasAccess(pid, user, groups);
    }

    public void clear() {
        synchronized (lastCleared) {
            this.pids2User2Access.clear();
            lastCleared.set(System.currentTimeMillis());
        }
    }

    public void setDefaultPermission(Permission defaultPermission) {
        this.defaultPermission = defaultPermission;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setSolrQueryLayer(SolrQueryLayerService solrQueryLayer) {
        this.solrQueryLayer = solrQueryLayer;
    }
}
