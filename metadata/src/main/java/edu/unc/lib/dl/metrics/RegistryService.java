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

import static io.dropwizard.metrics5.SharedMetricRegistries.getOrCreate;

import io.dropwizard.metrics5.MetricRegistry;

/**
 * A class that provides access to our application's metrics registry
 *
 * @author harring
 *
 */
public final class RegistryService {

    private static final MetricRegistry REGISTRY = getOrCreate("services-metrics");

    private static volatile RegistryService instance = null;

    private RegistryService() {
      // New instances should come from the singleton
    }

    /**
     * Create the instance
     *
     * @return the local object
     */
    public static synchronized RegistryService getInstance() {
        if (instance == null) {
            instance = new RegistryService();
            // associates a reporter with the current registry
            ReporterFactory.getOrCreateReporter();
        }
        return instance;
    }

    /**
     * Get the current metrics registry
     *
     * @return the current metrics registry
     */
    public MetricRegistry getRegistry() {
        return REGISTRY;
    }

}