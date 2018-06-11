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

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * A factory for creating gauges for reporting various metrics
 *
 * @author harring
 *
 */
public class GaugeFactory {

    private static final MetricRegistry registry = RegistryService.getInstance().getRegistry();

    private GaugeFactory() {

    }

    /**
     * Creates and registers a gauge under the given name
     *
     * @param metricName the name under which to register the gauge
     * @param duration the length of time elapsed in ms
     */
    public static void createDurationGauge(MetricName name, Long duration) {
        registry.register(name, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return duration;
            }
        });

    }

    /**
     * Creates and registers a generic gauge under the given name
     *
     * @param metricName the name under which to register the gauge
     * @param value the value to report
     */
    public static void createGauge(MetricName name, Object value) {
        registry.register(name, new Gauge<Object>() {
            @Override
            public Object getValue() {
                return value;
            }
        });

    }
}