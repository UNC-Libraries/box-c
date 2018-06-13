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

import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * A factory for creating histrograms for reporting various metrics
 *
 * @author harring
 *
 */
public class HistogramFactory {

    private static final MetricRegistry registry = RegistryService.getInstance().getRegistry();

    private HistogramFactory() {

    }

    /**
     * Creates and registers a histogram under the given name
     *
     * @param metricName the name under which to register the histogram
     */
    public static Histogram createHistogram(String name) {
        return registry.histogram(name);

    }

    /**
     * Creates and registers a histogram under the given metric name
     *
     * @param metricName the name under which to register the histogram
     */
    public static Histogram createHistogram(MetricName metricName) {
        return registry.histogram(metricName);

    }
}