package cdr.forms;

public class DepositResult {
	public static final int COMPLETE = 1;
	public static final int PENDING = 2;
	public static final int UNKNOWN = 0;
	public static final int FAILED = 3;
	private String objectPid;
	public String getObjectPid() {
		return objectPid;
	}
	public void setObjectPid(String objectPid) {
		this.objectPid = objectPid;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	private int status = UNKNOWN;
}
