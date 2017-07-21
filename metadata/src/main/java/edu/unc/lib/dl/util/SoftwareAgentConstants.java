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
 * Software Agents
 * @author bbpennel
 *
 */
public class SoftwareAgentConstants {
    private static String cdrVersion;

    private SoftwareAgentConstants() {
    }

    public static void setCdrVersion(String cdrVersion) {
        SoftwareAgentConstants.cdrVersion = cdrVersion;
    }

    public static String getCdrVersion() {
        return cdrVersion;
    }

    public enum SoftwareAgent {
        depositService("deposit"), servicesWorker("services-worker"), selfDepositForms(
                "forms"), servicesAPI("services"), fixityCheckingService(
                "fixity"), embargoUpdateService("embargo-update"), clamav(
                "clamav", "0.99"), FITS("fits", "0.8.5"), iRods("irods", "3.3"), jargon(
                "jargon", "3.2");

        private String value;
        private String version;

        private SoftwareAgent(String value) {
            this.value = value;
        }

        private SoftwareAgent(String value, String version) {
            this.value = value;
            this.version = version;
        }

        public String getFullname() {
            if (version == null) {
                return value + "-" + getCdrVersion();
            }
            return value + "-" + version;
        }
    }
}