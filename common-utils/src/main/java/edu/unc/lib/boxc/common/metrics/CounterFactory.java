package edu.unc.lib.boxc.common.metrics;

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