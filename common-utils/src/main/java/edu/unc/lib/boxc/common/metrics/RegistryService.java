package edu.unc.lib.boxc.common.metrics;

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