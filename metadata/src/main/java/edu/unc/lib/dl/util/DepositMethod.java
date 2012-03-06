package edu.unc.lib.dl.util;

public enum DepositMethod {
	Unspecified("Unspecified Method"), WebForm("CDR Web Form"), SWORD13("SWORD 1.3"), SWORD20("SWORD 2.0");
	
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
