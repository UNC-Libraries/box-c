package edu.unc.lib.boxc.deposit.api;

/**
 * Deposit method
 * @author bbpennel
 *
 */
public enum DepositMethod {
    Unspecified("Unspecified Method"),
    WebForm("CDR Web Form"),
    SWORD13("SWORD 1.3"),
    SWORD20("SWORD 2.0"),
    CDRAPI1("CDR API 1.0"),
    CDRCollector("CDR Collector 1.0"),
    BXC3_TO_5_MIGRATION_UTIL("BXC3 To BXC5 Migration Utility"),
    CDM_TO_BXC_MIGRATION("CDM To Box-c Migration");

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
