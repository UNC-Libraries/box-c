package edu.unc.lib.dl.cdr.services.model;

import java.util.Date;

import org.joda.time.format.ISODateTimeFormat;

import edu.unc.lib.dl.fedora.PID;

/**
 * Stores data related to a specific application of an enhancement
 * @author bbpennel
 *
 */
public class EnhancementApplication {
	//Subject of this enhancement
	private PID pid;
	//Last date on which this enhancement was applied
	private Date lastApplied;
	//Version of services when this enhancement was last run
	private String version;
	//Class of the enhancement applied
	private Class<?> enhancementClass;
	
	public PID getPid() {
		return pid;
	}
	public void setPid(PID pid) {
		this.pid = pid;
	}
	public Date getLastApplied() {
		return lastApplied;
	}
	public void setLastApplied(Date lastApplied) {
		this.lastApplied = lastApplied;
	}
	public void setLastAppliedFromISO8601(String lastApplied){
		this.lastApplied = ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(lastApplied).toDate();
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public Class<?> getEnhancementClass() {
		return enhancementClass;
	}
	public void setEnhancementClass(Class<?> enhancementClass) {
		this.enhancementClass = enhancementClass;
	}
}
