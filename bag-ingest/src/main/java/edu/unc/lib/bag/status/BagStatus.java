package edu.unc.lib.bag.status;

public class BagStatus {
	private String uuid;
	private String depositor;
	private String label;
	private String note;
	private int numObjects;
	private int numFiles;
	private int numOctets;
	private int startTime;
	private int endTime;
	private String state;
	private String errorMessage;
	private int finishedJobs;
	private int ingestedOctets;
	private int ingestedObjects;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getDepositor() {
		return depositor;
	}

	public void setDepositor(String depositor) {
		this.depositor = depositor;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public int getFinishedJobs() {
		return finishedJobs;
	}

	public void setFinishedJobs(int finishedJobs) {
		this.finishedJobs = finishedJobs;
	}

	public int getNumObjects() {
		return numObjects;
	}

	public void setNumObjects(int numObjects) {
		this.numObjects = numObjects;
	}

	public int getNumFiles() {
		return numFiles;
	}

	public void setNumFiles(int numFiles) {
		this.numFiles = numFiles;
	}

	public int getNumOctets() {
		return numOctets;
	}

	public void setNumOctets(int numOctets) {
		this.numOctets = numOctets;
	}

	public int getIngestedOctets() {
		return ingestedOctets;
	}

	public void setIngestedOctets(int ingestedOctets) {
		this.ingestedOctets = ingestedOctets;
	}

	public int getIngestedObjects() {
		return ingestedObjects;
	}

	public void setIngestedObjects(int ingestedObjects) {
		this.ingestedObjects = ingestedObjects;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public BagStatus() {
	}

}
