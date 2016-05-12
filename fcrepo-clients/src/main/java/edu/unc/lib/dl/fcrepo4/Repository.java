package edu.unc.lib.dl.fcrepo4;

import edu.unc.lib.dl.fedora.PID;

public class Repository {
	private String depositRecordBase;
	private String vocabulariesBase;
	private String contentBase;
	private String agentsBase;
	private String policiesBase;
	
	public String getDepositRecordsBase() {
		return depositRecordBase;
	}
	
	/*
	 * returns the path for a deposit record by uuid
	 */
	public String getDepositRecordPath(String uuid) {
		return depositRecordBase + uuid;
	}
	
	public DepositRecord getDepositRecord(String uuid) {
		return null;
	}
	
	public String getVocabulariesBase() {
		return vocabulariesBase;
	}
	
	public String getContentBase() {
		return contentBase;
	}
	
	public String getContentPath(PID pid) {
		return null;
	}
	
	public ContentObject getContentObject(PID pid) {
		return null;
	}
	
	public ContentObject getContentObject(String path) {
		return null;
	}
	
	public String getAgentsBase() {
		return agentsBase;
	}
	
	public String getPoliciesBase() {
		return policiesBase;
	}

	public String getDepositRecordBase() {
		return depositRecordBase;
	}

	public void setDepositRecordBase(String depositRecordBase) {
		this.depositRecordBase = depositRecordBase;
	}

	public void setVocabulariesBase(String vocabulariesBase) {
		this.vocabulariesBase = vocabulariesBase;
	}

	public void setContentBase(String contentBase) {
		this.contentBase = contentBase;
	}

	public void setAgentsBase(String agentsBase) {
		this.agentsBase = agentsBase;
	}

	public void setPoliciesBase(String policiesBase) {
		this.policiesBase = policiesBase;
	}
}
