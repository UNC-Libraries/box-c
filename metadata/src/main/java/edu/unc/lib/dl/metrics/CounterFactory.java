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
package edu.unc.lib.dl.metrics;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * A factory for creating counters for reporting various metrics
 *
 * @author harring
 *
 */
public class CounterFactory {

    private static final MetricRegistry registry = RegistryService.getInstance().getRegistry();

    private CounterFactory() {

    }

    /**
     * Creates and registers a counter for the given class
     *
     * @param className the class name under which to register the counter
     * @param metricNames any further specific name(s) for the counter (Optional)
     */
    public static Counter createCounter(Class<?> className, String... metricNames) {
        return registry.counter(MetricRegistry.name(className, metricNames));
    }

}