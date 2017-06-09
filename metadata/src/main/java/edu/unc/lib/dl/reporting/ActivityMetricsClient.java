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
package edu.unc.lib.dl.reporting;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_METRICS_PREFIX;
import static edu.unc.lib.dl.util.RedisWorkerConstants.OPERATION_METRICS_PREFIX;

import java.text.SimpleDateFormat;
import java.util.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author bbpennel
 * @date Sep 30, 2015
 */
public class ActivityMetricsClient {

    private static SimpleDateFormat metricsDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd");

    private JedisPool jedisPool;

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void incrMoves() {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(OPERATION_METRICS_PREFIX + date, "moves", 1);
        }
    }

    public void incrFinishedEnhancement(String className) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(OPERATION_METRICS_PREFIX + date, "finished-enh:" + className, 1);
        }
    }

    public void incrFailedEnhancement(String className) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(OPERATION_METRICS_PREFIX + date, "failed-enh:" + className, 1);
        }
    }

    public void incrFailedDeposit() {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(DEPOSIT_METRICS_PREFIX + date, "failed", 1);
        }
    }

    public void incrFailedDepositJob(String className) {
        incrFailedDeposit();

        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(DEPOSIT_METRICS_PREFIX + date, "failed-job:" + className, 1);
        }
    }

    public void incrFinishedDeposit() {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());
            jedis.hincrBy(DEPOSIT_METRICS_PREFIX + date, "finished", 1);
        }
    }

    public void incrDepositFileThroughput(String uuid, long bytes) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());

            jedis.hincrBy(DEPOSIT_METRICS_PREFIX + date + ":" + uuid, "throughput-files", 1);
            jedis.hincrBy(DEPOSIT_METRICS_PREFIX + date + ":" + uuid, "throughput-bytes", bytes);
        }
    }

    public void setDepositDuration(String uuid, long milliseconds) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());

            jedis.hset(DEPOSIT_METRICS_PREFIX + date + ":" + uuid, "duration", Long.toString(milliseconds));
        }
    }

    public void setQueuedDepositDuration(String uuid, long milliseconds) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String date = metricsDateFormat.format(new Date());

            jedis.hset(DEPOSIT_METRICS_PREFIX + date + ":" + uuid, "queued-duration", Long.toString(milliseconds));
        }
    }
}
