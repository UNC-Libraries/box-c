package edu.unc.lib.boxc.deposit.impl.model;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JobField;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JOB_STATUS_PREFIX;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author bbpennel
 */
public class JobStatusFactoryTest {

    @Mock
    private JedisPool jedisPool;
    @Mock
    private Jedis jedis;

    private String jobUUID;

    private JobStatusFactory statusFactory;

    @BeforeEach
    public void setup() {
        initMocks(this);

        when(jedisPool.getResource()).thenReturn(jedis);

        jobUUID = UUID.randomUUID().toString();

        statusFactory = new JobStatusFactory();
        statusFactory.setSocketTimeoutDelay(1);
        statusFactory.setSocketTimeoutRetries(4);
        statusFactory.setJedisPool(jedisPool);
    }

    @Test
    public void incrCompletionSuccess() {
        statusFactory.incrCompletion(jobUUID, 1);
        statusFactory.incrCompletion(jobUUID, 2);

        verify(jedis).hincrBy(JOB_STATUS_PREFIX + jobUUID, JobField.num.name(), 1);
        verify(jedis).hincrBy(JOB_STATUS_PREFIX + jobUUID, JobField.num.name(), 2);
    }

    @Test
    public void incrCompletionInterruptRecovery() {
        SocketTimeoutException cause = new SocketTimeoutException("Timed out");
        JedisConnectionException ex = new JedisConnectionException(cause);
        when(jedis.hincrBy(anyString(), anyString(), anyLong())).thenThrow(ex).thenReturn(1L);

        statusFactory.incrCompletion(jobUUID, 1);

        verify(jedis, times(2)).hincrBy(JOB_STATUS_PREFIX + jobUUID, JobField.num.name(), 1);
    }

    @Test
    public void incrCompletionInterruptFail() {
        Assertions.assertThrows(JedisConnectionException.class, () -> {
            SocketTimeoutException cause = new SocketTimeoutException("Timed out");
            JedisConnectionException ex = new JedisConnectionException(cause);
            when(jedis.hincrBy(anyString(), anyString(), anyLong())).thenThrow(ex);

            statusFactory.incrCompletion(jobUUID, 1);
        });
    }

    @Test
    public void incrCompletionUnexpectedException() {
        Assertions.assertThrows(RepositoryException.class, () -> {
            Exception ex = new RepositoryException("Oops");
            when(jedis.hincrBy(anyString(), anyString(), anyLong())).thenThrow(ex).thenReturn(1L);

            statusFactory.incrCompletion(jobUUID, 1);
        });

    }
}
