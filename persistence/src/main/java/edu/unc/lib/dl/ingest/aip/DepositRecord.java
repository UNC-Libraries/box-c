package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.util.UUID;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PackagingType;

public class DepositRecord {
	private PackagingType packagingType = null;
	private String packagingSubType = null;
	private DepositMethod method = DepositMethod.Unspecified;
	private Agent depositedBy = null;
	private Agent owner = null;
	public Agent getOwner() {
		return owner;
	}
	private String onBehalfOf = null;
	private String message = null;
	public DepositRecord(Agent depositedBy, Agent owner, DepositMethod method) {
		this.depositedBy = depositedBy;
		this.method = method;
		this.owner = owner;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	private File manifest = null;
	private PID pid = new PID(String.format("uuid:%1$s", UUID.randomUUID()));
	public PackagingType getPackagingType() {
		return packagingType;
	}
	public void setPackagingType(PackagingType packagingType) {
		this.packagingType = packagingType;
	}
	public String getPackagingSubType() {
		return packagingSubType;
	}
	public void setPackagingSubType(String packagingSubType) {
		this.packagingSubType = packagingSubType;
	}
	public DepositMethod getMethod() {
		return method;
	}
	public Agent getDepositedBy() {
		return depositedBy;
	}
	public String getOnBehalfOf() {
		return onBehalfOf;
	}
	public void setOnBehalfOf(String onBehalfOf) {
		this.onBehalfOf = onBehalfOf;
	}
	public File getManifest() {
		return manifest;
	}
	public void setManifest(File manifest) {
		this.manifest = manifest;
	}
	public PID getPid() {
		return pid;
	}
	public void setPid(PID pid) {
		this.pid = pid;
	}
}
