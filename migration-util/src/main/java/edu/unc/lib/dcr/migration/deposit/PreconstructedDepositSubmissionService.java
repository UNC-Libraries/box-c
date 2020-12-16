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
package edu.unc.lib.dcr.migration.deposit;

import static edu.unc.lib.dl.util.DepositMethod.BXC3_TO_5_MIGRATION_UTIL;
import static edu.unc.lib.dl.util.PackagingType.BAG_WITH_N3;

import java.io.Closeable;
import java.io.IOException;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.services.ingest.PreconstructedDepositHandler;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Service which submits a preconstructed deposit for ingestion
 *
 * @author bbpennel
 */
public class PreconstructedDepositSubmissionService implements Closeable {

    protected static final String EMAIL_SUFFIX = "@ad.unc.edu";

    private DepositStatusFactory depositStatusFactory;

    private JedisPool jedisPool;

    public PreconstructedDepositSubmissionService(String redisHost, int redisPort) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(15);
        jedisPoolConfig.setMaxTotal(25);
        jedisPoolConfig.setMinIdle(2);

        jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort);

        depositStatusFactory = new DepositStatusFactory();
        depositStatusFactory.setJedisPool(jedisPool);
    }

    /**
     * Submits the deposit with the given pid for deposit into the specified destination.
     *
     * @param depositorName name of the user performing the deposit
     * @param depositorGroup Group(s) the user belongs to
     * @param depositPid pid of the deposit to submit
     * @param destination pid of the destination object
     * @param displayLabel deposit label that will be used by the status monitor
     * @return result code
     */
    public int submitDeposit(String depositorName, String depositorGroup, PID depositPid, PID destination,
                             String displayLabel) {
        AgentPrincipals principals = new AgentPrincipals(depositorName, new AccessGroupSet(depositorGroup));

        DepositData depositData = new DepositData(null, null, BAG_WITH_N3,
                BXC3_TO_5_MIGRATION_UTIL.getLabel(), principals);
        depositData.setDepositorEmail(depositorName + EMAIL_SUFFIX);
        depositData.setOverrideTimestamps(true);
        depositData.setPriority(Priority.low);
        if (!displayLabel.equals("")) {
            depositData.setSlug(displayLabel);
        }

        PreconstructedDepositHandler depositHandler = new PreconstructedDepositHandler(depositPid);
        depositHandler.setDepositStatusFactory(depositStatusFactory);
        try {
            depositHandler.doDeposit(destination, depositData);
        } catch (DepositException e) {
            throw new RepositoryException("Failed to submit deposit", e);
        }

        return 0;
    }

    @Override
    public void close() throws IOException {
        jedisPool.close();
    }
}
