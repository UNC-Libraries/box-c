package edu.unc.lib.boxc.search.solr.ranges;

import edu.unc.lib.boxc.search.api.ranges.RangeValue;

/**
 * A range for entries which do not have a value assigned
 *
 * @author bbpennel
 */
public class UnknownRange implements RangeValue {
    public final static String UNKNOWN_VALUE = "unknown";

    public UnknownRange() {
    }

    @Override
    public String getParameterValue() {
        return UNKNOWN_VALUE;
    }

    @Override
    public RangeValue clone() {
        return new UnknownRange();
    }

    /**
     * @param value
     * @return true if the range value is unknown
     */
    public static boolean isUnknown(String value) {
        return UNKNOWN_VALUE.equals(value);
    }
}
