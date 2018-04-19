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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.Slf4jReporter;

/**
 * A factory for creating class-specific metrics reporters
 *
 * @author harring
 *
 */
public class ReporterFactory {

    private ReporterFactory() {

    }

    public static Slf4jReporter createReporter(Logger logger, long timePeriod, TimeUnit timeUnits,
            MetricRegistry registry) {

        final Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
                .outputTo(logger)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(timePeriod, timeUnits);

        return reporter;
    }

}
