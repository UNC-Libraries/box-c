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
package edu.unc.lib.deposit.work;

import net.greghaines.jesque.Config;
import redis.clients.jedis.Jedis;

/**
 * @author bbpennel
 */
public class JedisFactory {
    private JedisFactory() {
    }

    /**
     * Create a connection to Redis from the given Config.
     * 
     * @param config
     *            the location of the Redis server
     * @return a new connection
     */
    public static Jedis createJedis(final Config config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final Jedis jedis = new Jedis(config.getHost(), config.getPort(),
                config.getTimeout());
        if (config.getPassword() != null) {
            jedis.auth(config.getPassword());
        }
        jedis.select(config.getDatabase());
        return jedis;
    }
}
