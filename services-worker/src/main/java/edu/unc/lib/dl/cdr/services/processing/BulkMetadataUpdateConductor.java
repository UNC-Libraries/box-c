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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.WorkerPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.update.BulkMetadataUpdateJob;
import edu.unc.lib.dl.util.RedisWorkerConstants;

/**
 * @author bbpennel
 * @date Jul 31, 2015
 */
public class BulkMetadataUpdateConductor {
    private static final Logger log = LoggerFactory.getLogger(BulkMetadataUpdateConductor.class);

    private net.greghaines.jesque.client.Client jesqueClient;
    private WorkerPool workerPool;
    private String queueName;
    private JedisPool jedisPool;

    public void add(String email, String username, Collection<String> groups, File importFile,
            String originalFilename) {
        add(null, email, username, groups, importFile, originalFilename);
    }

    public void add(String updateId, String email, String username, Collection<String> groups,
            File importFile, String originalFilename) {
        Job job = new Job(BulkMetadataUpdateJob.class.getName(), updateId, email, username, groups,
                importFile.getAbsolutePath(), originalFilename);
        jesqueClient.enqueue(queueName, job);
    }

    public void resumeIncompleteUpdates() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> incompleteUpdates = jedis.keys(RedisWorkerConstants.BULK_UPDATE_PREFIX + "*");

            for (String incomplete : incompleteUpdates) {
                Map<String, String> updateValues = jedis.hgetAll(incomplete);

                String updateId = incomplete.split(":", 2)[1];

                // If the import file doesn't exist, then can't resume
                File importFile = new File(updateValues.get("filePath"));
                if (!importFile.exists()) {
                    log.warn("Failed to resume update {} for user {} because the file no longer existed",
                            updateValues.get("originalFilename"), updateValues.get("user"));
                    jedis.del(incomplete);
                    jedis.del(RedisWorkerConstants.BULK_RESUME_PREFIX + updateId);
                } else {
                    add(updateId, updateValues.get("email"), updateValues.get("user"),
                            Arrays.asList(updateValues.get("groups").split(" ")),
                            importFile, updateValues.get("originalFilename"));
                }
            }
        }
    }

    public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
        this.jesqueClient = jesqueClient;
    }

    public void setWorkerPool(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void init() {
        resumeIncompleteUpdates();
    }

    public void destroy() {
        workerPool.end(true);
    }
}
