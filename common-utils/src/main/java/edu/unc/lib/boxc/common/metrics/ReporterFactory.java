package edu.unc.lib.boxc.common.metrics;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Slf4jReporter;

/**
 * A factory for creating a metrics reporter. Each created reporter is a singleton.
 *
 * @author harring
 *
 */
public class ReporterFactory {

    private static final RegistryService registryService = RegistryService.getInstance();
    private static final String METRICS = System.getProperty("metrics.report.name", "metrics");
    private static ScheduledReporter reporterInstance = null;

    private static final Logger LOGGER = getLogger(METRICS);
    private static final long TIME_PERIOD = Long.parseLong(System.getProperty("metrics.report.time", "60"));
    private static final TimeUnit TIME_UNITS = TimeUnit.SECONDS;

    private ReporterFactory() {

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
                    .prefixedWith(METRICS)
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
