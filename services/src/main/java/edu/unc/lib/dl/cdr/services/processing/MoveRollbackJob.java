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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * Task which seeks out any previously failed move operations and attempts to roll them back
 *
 * @author bbpennel
 * @date Feb 12, 2015
 */
public class MoveRollbackJob {

    private static final Logger log = LoggerFactory.getLogger(MoveRollbackJob.class);


    private final String REMOVE_CHILD_QUERY = "select $parent $child from <%1$s> where $parent <%2$s> $child;";

    /**
     * Seeks out containers with leftover removed children placeholders and attempts to roll back move operations
     */
    public void rollbackAllFailed() {

        Map<PID, List<PID>> removedMap = getRemovedMap();
        if (removedMap.size() == 0) {
            log.info("No failed move operations were found");
        }
        log.info("Attempting to rollback move operations from {} sources", removedMap.size());

        for (Entry<PID, List<PID>> removedEntry : removedMap.entrySet()) {
//            try {
//                objectManager.rollbackMove(removedEntry.getKey(), removedEntry.getValue());
//            } catch (IngestException e) {
//                log.error("Failed to rollback previous move operation from {}", removedEntry.getKey(), e);
//            }
        }

    }

    private Map<PID, List<PID>> getRemovedMap() {
//        String query = String.format(
//                REMOVE_CHILD_QUERY,
//                queryService.getResourceIndexModelUri(),
//                ContentModelHelper.Relationship.removedChild.toString());
//
//        Map<PID, List<PID>> parentToRemoved = new HashMap<>();
//        List<List<String>> response = queryService.queryResourceIndex(query);
//
//        if (!response.isEmpty()) {
//            for (List<String> values : response) {
//                PID parent = new PID(values.get(0));
//                PID child = new PID(values.get(1));
//
//                List<PID> removed = parentToRemoved.get(parent);
//                if (removed == null) {
//                    removed = new ArrayList<>();
//                    parentToRemoved.put(parent, removed);
//                }
//                removed.add(child);
//            }
//        }
//        return parentToRemoved;
        return null;
    }

}
