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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.util.RedisWorkerConstants.JOB_STATUS_PREFIX;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.SocketTimeoutException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobField;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

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

    @Before
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
        when(jedis.hincrBy(anyString(), anyString(), anyInt())).thenThrow(ex).thenReturn(1l);

        statusFactory.incrCompletion(jobUUID, 1);

        verify(jedis, times(2)).hincrBy(JOB_STATUS_PREFIX + jobUUID, JobField.num.name(), 1);
    }

    @Test(expected = JedisConnectionException.class)
    public void incrCompletionInterruptFail() {
        SocketTimeoutException cause = new SocketTimeoutException("Timed out");
        JedisConnectionException ex = new JedisConnectionException(cause);
        when(jedis.hincrBy(anyString(), anyString(), anyInt())).thenThrow(ex);

        statusFactory.incrCompletion(jobUUID, 1);
    }

    @Test(expected = RepositoryException.class)
    public void incrCompletionUnexpectedException() {
        Exception ex = new RepositoryException("Oops");
        when(jedis.hincrBy(anyString(), anyString(), anyInt())).thenThrow(ex).thenReturn(1l);

        statusFactory.incrCompletion(jobUUID, 1);
    }
}
