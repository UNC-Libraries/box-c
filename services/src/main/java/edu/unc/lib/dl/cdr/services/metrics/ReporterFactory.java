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
package edu.unc.lib.dl.cdr.services.metrics;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Slf4jReporter;

/**
 * A factory for creating a metrics reporter for the services application. Both the factory
 * and the reporter are singletons.
 *
 * @author harring
 *
 */
public class ReporterFactory {

    private static final RegistryService registryService = RegistryService.getInstance();
    private static final String SERVICES_METRICS = "services-metrics";
    private static volatile ReporterFactory factoryInstance = null;
    private static ScheduledReporter reporterInstance = null;

    private static final Logger LOGGER = getLogger("services-metrics");
    private static final long TIME_PERIOD = 1;
    private static final TimeUnit TIME_UNITS = TimeUnit.MINUTES;

    private ReporterFactory() {

    }

        /**
         * Create the factory instance
         *
         * @return the local object
         */
        public synchronized static ReporterFactory getInstance() {
            ReporterFactory local = factoryInstance;
            if (local == null) {
                local = new ReporterFactory();
                factoryInstance = local;
            }
            return local;
        }

        /**
         * Get the metrics reporter for this application, or create a new one if none exists.
         *
         * @return the slf4j metrics reporter for this application
         */
    public static ScheduledReporter getOrCreateReporter() {
        ScheduledReporter reporter = reporterInstance;
        if (reporter == null) {
            reporter = Slf4jReporter.forRegistry(registryService.getRegistry())
                    .prefixedWith(SERVICES_METRICS)
                    .outputTo(LOGGER)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
            reporter.start(TIME_PERIOD, TIME_UNITS);
            reporterInstance = reporter;
        }
        return reporter;
    }

}
