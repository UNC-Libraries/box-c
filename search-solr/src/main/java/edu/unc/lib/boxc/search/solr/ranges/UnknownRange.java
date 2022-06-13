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
