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
package edu.unc.lib.dl.util;

/**
 * Deposit method
 * @author bbpennel
 *
 */
public enum DepositMethod {
    Unspecified("Unspecified Method"), WebForm("CDR Web Form"), SWORD13(
            "SWORD 1.3"), SWORD20("SWORD 2.0"), CDRAPI1("CDR API 1.0"), CDRCollector(
            "CDR Collector 1.0");

    private String label;

    DepositMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
