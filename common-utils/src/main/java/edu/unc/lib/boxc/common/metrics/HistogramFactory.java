package edu.unc.lib.boxc.common.metrics;

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