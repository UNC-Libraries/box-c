package edu.unc.lib.boxc.model.api;


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
        depositService("bxc-deposit", true),
        servicesWorker("services-worker", "3.0"),
        selfDepositForms("forms"),
        servicesAPI("bxc-services", true),
        fixityCheckingService("fixity", "3.0"),
        embargoUpdateService("embargo-update", "3.0"),
        bxc3ToBxc5MigrationUtil("bxc-migration-util", true),
        cdmToBxcMigrationUtil("cdm-to-boxc-migration-util", "1.0"),
        clamav("clamav", "0.99"),
        depositBxc3("deposit", "3.0"),
        FITS("fits", "1.0.6"),
        curatorsWorkbench("curators-workbench"),
        embargoExpirationService("bxc-embargo-update-service", true);

        private String value;
        private String version;
        private boolean useCdrVersion = false;

        private SoftwareAgent(String value) {
            this.value = value;
        }

        private SoftwareAgent(String value, boolean useCdrVersion) {
            this.value = value;
            this.useCdrVersion = useCdrVersion;
        }

        private SoftwareAgent(String value, String version) {
            this.value = value;
            this.version = version;
        }

        public String getFullname() {
            if (version == null) {
                if (useCdrVersion) {
                    return value + "-" + getCdrVersion();
                } else {
                    return value;
                }
            }

            return value + "-" + version;
        }

        public String getValue() {
            return value;
        }
    }
}