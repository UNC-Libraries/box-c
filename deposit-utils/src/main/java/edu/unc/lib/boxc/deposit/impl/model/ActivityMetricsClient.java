package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_METRICS_PREFIX;

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
