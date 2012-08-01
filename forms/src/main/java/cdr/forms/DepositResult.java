package cdr.forms;

public class DepositResult {
	public static enum Status {COMPLETE, PENDING, UNKNOWN, FAILED};
	private String objectPid;
	public String getObjectPid() {
		return objectPid;
	}
	public void setObjectPid(String objectPid) {
		this.objectPid = objectPid;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	private Status status = Status.UNKNOWN;
}
