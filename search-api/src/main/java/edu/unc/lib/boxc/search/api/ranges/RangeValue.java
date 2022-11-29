package edu.unc.lib.boxc.search.api.ranges;

/**
 * Object which identifies a range of values for retrieval
 *
 * @author bbpennel
 */
public interface RangeValue {
    /**
     * @return the value of this range represented as a String parameter
     */
    public String getParameterValue();

    public RangeValue clone();
}
