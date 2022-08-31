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
package edu.unc.lib.boxc.indexing.solr.utils;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

import java.util.List;
import java.util.Map;

/**
 * Service which produces a sortable identifier for ordering members within their parent
 *
 * @author bbpennel
 */
public class MemberOrderService {
    private final static int CACHE_SIZE = 64;
    private Map<PID, List<PID>> cache;

    public void init() {
        var mapBuilder = new ConcurrentLinkedHashMap.Builder<PID, List<PID>>();
        mapBuilder.maximumWeightedCapacity(CACHE_SIZE);
        cache = mapBuilder.build();
    }

    /**
     * Gets the order id for the provided member within its parent
     * @param memberObj the member object
     * @return the sortable order value for the member relative to its siblings within its parent.
     *      Null if its parent does not support ordering, or if the member is not ordered within its parent.
     */
    public Integer getOrderValue(RepositoryObject memberObj) {
        if (memberObj instanceof FileObject) {
            var parentPid = memberObj.getParentPid();
            List<PID> order;
            // Get order info from cache if available
            if (cache.containsKey(parentPid)) {
                order = cache.get(parentPid);
            } else {
                // Otherwise, retrieve it from the parent and cache it
                var parent = (WorkObject) memberObj.getParent();
                order = parent.getMemberOrder();
                cache.put(parentPid, order);
            }
            var index = order.indexOf(memberObj.getPid());
            return index == -1 ? null : index;
        }
        return null;
    }

    /**
     * Invalidate a cached member entry
     * @param pid
     */
    public void invalidate(PID pid) {
        cache.remove(pid);
    }

    /**
     * Invalidate all cached entries
     */
    public void invalidateAll() {
        cache.clear();
    }
}
