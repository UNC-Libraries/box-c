package edu.unc.lib.dl.util;

public class SoftwareAgentConstants {
	private static String cdrVersion;
	
	public static void setCdrVersion(String cdrVersion) {
		SoftwareAgentConstants.cdrVersion = cdrVersion;
	}
	
	public static String getCdrVersion() {
		return cdrVersion;
	}

	public enum SoftwareAgent {
		depositService("deposit", cdrVersion), 
		servicesWorker("services-worker", cdrVersion), 
		selfDepositForms("forms", cdrVersion), 
		servicesAPI("services", cdrVersion), 
		fixityCheckingService("fixity", cdrVersion), 
		embargoUpdateService("embargo-update", cdrVersion), 
		clamav("clamav", "0.99"),
		FITS("fits", "0.8.5"),
		iRods("irods", "3.3"),
		jargon("jargon", "3.2");
		
		private String value;
		private String version;

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