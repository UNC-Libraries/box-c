package edu.unc.lib.dl.services;

import java.util.Date;

import org.irods.jargon.core.protovalues.ErrorEnum;

import edu.unc.lib.dl.fedora.PID;

public class FixityLogEntry {

	public static enum Result { OK, FAILED, ERROR, MISSING }

	private Date time;
	private String obj;
	private String resc;
	private String file;
	private String expected;
	private Result result;
	private Integer code;
	private String version;
	private Integer elapsed;

	/**
	 * The time at which the fixity log entry was recorded.
	 */

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	/**
	 * The iRODS object for which the fixity log entry was recorded.
	 */

	public String getObj() {
		return obj;
	}

	public void setObj(String obj) {
		this.obj = obj;
	}

	/**
	 * The resource on which fixity checking was done.
	 */

	public String getResc() {
		return resc;
	}

	public void setResc(String resc) {
		this.resc = resc;
	}

	/**
	 * The actual file path checked. If the object was not in the iCAT on this resource, this is null.
	 */

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * The expected checksum. If no checksum was recorded or the object was not in the iCAT, this is null.
	 */

	public String getExpected() {
		return expected;
	}

	public void setExpected(String expected) {
		this.expected = expected;
	}

	/**
	 * The result of fixity checking. Possible values:
	 * 
	 * OK - The checksum of the object was calculated successfully and it matched the checksum recorded in the iCAT.
	 * FAILED - The checksum of the object was calculated successfully, but it didn't match the checksum recorded in the iCAT.
	 * ERROR - An error occurred while calculating the checksum. A description of the error’s result code can be found using the ierror command.
	 * MISSING - Checksum verification couldn't be performed because the object isn't in the specified resource.
	 */

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	/**
	 * The iRODS error code, if any was recorded. Otherwise, null.
	 */

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	/**
	 * The iRODS error corresponding to the error code. If there is no corresponding iRODS error code, null.
	 */

	public ErrorEnum getError() {
		if (code == null)
			return null;

		try {
			return ErrorEnum.valueOf(code.intValue());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * The iRODS release version used to generate this log entry.
	 */
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	/**
	 * Elapsed time for fixity verification in seconds (if performed).
	 */
	
	public Integer getElapsed() {
		return elapsed;
	}
	
	public void setElapsed(Integer elapsed) {
		this.elapsed = elapsed;
	}

	/**
	 * If the object path has a PID, return it. Otherwise, return null.
	 */

	public PID getPID() {

		int pidStart = obj.indexOf("uuid_");

		if (pidStart == -1)
			return null;

		int pidEnd = obj.indexOf("+", pidStart);

		if (pidEnd == -1)
			return new PID(obj.substring(pidStart).replace("_", ":"));
		else
			return new PID(obj.substring(pidStart, pidEnd).replace("_", ":"));

	}

	/**
	 * If the object path has a PID and a datastream, return the datastream. Otherwise, return null.
	 */

	public String getDatastream() {

		int pidStart = obj.indexOf("uuid_");

		if (pidStart == -1)
			return null;

		int pidEnd = obj.indexOf("+", pidStart);

		if (pidEnd == -1)
			return null;

		int dsidStart = pidEnd + 1;
		int dsidEnd = obj.indexOf("+", dsidStart);

		if (dsidEnd == -1)
			return obj.substring(dsidStart);
		else
			return obj.substring(dsidStart, dsidEnd);

	}
	
	@Override
	public String toString() {
		return "FixityLogEntry [" +
				"time=" + time + ", " +
				"obj=" + obj + ", " +
				"resc=" + resc + ", " +
				"file=" + file + ", " +
				"expected=" + expected + ", " +
				"result=" + result + ", " +
				"code=" + code + ", " +
				"version=" + version +
				"]";
	}

}
