package edu.unc.lib.dl.util;

public class SoftwareAgentConstants {
	public enum SoftwareAgent {
		depositService("deposit"), 
		servicesWorker("services-worker"), 
		selfDepositForms("forms"), 
		servicesAPI("services"), 
		fixityCheckingService("fixity"), 
		embargoUpdateService("embargo-update"), 
		clamav("clamav-0.99"),
		FITS("fits-0.8.5"),
		iRods("irods-3.3"),
		jargon("jargon-3.2");
		
		private String value;

		private SoftwareAgent(String value) {
			this.value = value;
		}
		
		public String getValue() {
	        return value;
	    }
	}
}