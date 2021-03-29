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
package edu.unc.lib.dl.acl.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.InetAddresses;

/**
 * Configuration for an IP Address based patron group
 *
 * @author bbpennel
 */
public class IPAddressPatronPrincipalConfig {

    private String name;
    private String principal;
    private String ipInclude;
    private List<IPRange> includeRanges;

    public String getName() {
        return name;
    }

    @JsonProperty(required = true)
    public void setName(String name) {
        this.name = name;
    }

    public String getPrincipal() {
        return principal;
    }

    @JsonProperty(required = true)
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    @JsonIgnore
    public String getIpInclude() {
        return ipInclude;
    }

    @JsonProperty(required = true)
    public void setIpInclude(String ipInclude) {
        this.ipInclude = ipInclude;

        includeRanges = Arrays.stream(ipInclude.split(","))
                .map(IPRange::new)
                .collect(Collectors.toList());
    }

    /**
     * @param ipAddr IP address represented as a BigInteger. See
     * {@link IPAddressPatronPrincipalConfig#ipToBigInteger(String)}
     * @return returns true if the given IP address is within the range of this patron group
     */
    public boolean inRange(BigInteger ipAddr) {
        return includeRanges.stream().anyMatch(range -> range.inRange(ipAddr));
    }

    /**
     * Converts a string IP address, in IPv4 or IPv6 form, to a BigInteger
     * @param addr IP address
     * @return BigInteger representing the IP address
     */
    public static BigInteger ipToBigInteger(String addr) {
        InetAddress a = InetAddresses.forString(addr);
        return new BigInteger(1, a.getAddress());
    }

    /**
     * An IP address range. Can support exact matches, or ranges.
     * Open ended ranges are not supported. Ranges are inclusive.
     * @author bbpennel
     */
    private static class IPRange {
        private BigInteger start;
        // End will be null for exact IP matchers
        private BigInteger end;

        public IPRange(String range) {
            Assert.notNull(range, "Range field must not be null");
            String[] parts = range.split("-", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException(
                        "IP Address range may only contain two hyphen separated parts: " + range);
            }
            if (StringUtils.isBlank(parts[0])) {
                throw new IllegalArgumentException(
                        "Start of IP Address range must not be blank: " + range);
            }
            start = ipToBigInteger(parts[0]);
            if (parts.length == 2) {
                if (StringUtils.isBlank(parts[1])) {
                    throw new IllegalArgumentException(
                            "Must specify end to the range or leave off '-': " + range);
                }
                end = ipToBigInteger(parts[1]);
            }
        }

        /**
         * Address is in range if it is greater than or equal to the start of the range, and
         * less than or equal to the end of the range. If no end range is specified,
         * then the address must be equal to the start of the range.
         *
         * @param addr
         * @return true if the address is within this ip range
         */
        public boolean inRange(BigInteger addr) {
            int startCompare = addr.compareTo(start);
            if (startCompare == -1) {
                return false;
            }
            if (end == null) {
                return startCompare == 0;
            }
            return addr.compareTo(end) <= 0;
        }
    }
}
