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
import org.apache.commons.lang3.StringUtils;

/**
 * A range defined by a pair of values, a left hand and a right hand
 *
 * @author bbpennel
 */
public class RangePair implements RangeValue {
    // A null in either side of the pair indicates no restriction
    private String leftHand;
    private String rightHand;

    public RangePair() {
    }

    public RangePair(String pairString) {
        String[] pairParts = pairString.split(",", 2);
        checkValidRangePair(pairParts[0], pairParts[1]);
        if (pairParts[0].length() > 0) {
            this.leftHand = pairParts[0];
        } else {
            this.leftHand = null;
        }
        if (pairParts[1].length() > 0) {
            this.rightHand = pairParts[1];
        } else {
            this.rightHand = null;
        }
    }

    public RangePair(String leftHand, String rightHand) {
        this.leftHand = leftHand;
        this.rightHand = rightHand;
    }

    public RangePair(RangePair rangePair) {
        this.leftHand = rangePair.getLeftHand();
        this.rightHand = rangePair.getRightHand();
    }

    @Override
    public RangePair clone() {
        return new RangePair(this);
    }

    public String getLeftHand() {
        return leftHand;
    }

    public void setLeftHand(String leftHand) {
        checkValidRangePair(leftHand, getRightHand());
        this.leftHand = leftHand;
    }

    public String getRightHand() {
        return rightHand;
    }

    public void setRightHand(String rightHand) {
        checkValidRangePair(getLeftHand(), rightHand);
        this.rightHand = rightHand;
    }

    @Override
    public String getParameterValue() {
        if (leftHand == null) {
            if (rightHand == null) {
                return "";
            }
            return "," + rightHand;
        }
        if (rightHand == null) {
            return leftHand + ",";
        }
        return leftHand + "," + rightHand;
    }

    private boolean validRangeValue(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void checkValidRangePair(String start, String end) {
        boolean validStart = validRangeValue(start);
        boolean validEnd = validRangeValue(end);

        // Checks if one side of the range pair is valid and the other is empty
        // This results in queries such as [2020 TO *] or [* TO 2020]
        if ((validStart && StringUtils.isEmpty(end)) || (StringUtils.isEmpty(start) && validEnd)) {
            return;
        }

        if (!validStart && !validEnd) {
            throw new IllegalArgumentException("Invalid search range. Start Value: '" + start + "', End Value: " +
                    "'" + end + "'");
        }

        if (Integer.parseInt(start) > Integer.parseInt(end)) {
            throw new IllegalArgumentException("Invalid search range. Start value: '" + start +
                    "', is greater than end value: '" + end + "'");
        }
    }
}
