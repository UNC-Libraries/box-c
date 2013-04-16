package edu.unc.lib.dl.fedora;

/**
 * Datastream aware PID object
 * 
 * @author bbpennel
 *
 */
public class DatastreamPID extends PID {
	private static final long serialVersionUID = -668408790779214575L;
	private String datastream;
	
	public DatastreamPID(String pid) {
		super(pid);
		
		datastream = null;
		
		int index = this.pid.indexOf('/');
		if (index != -1){
			if (index != this.pid.length() - 1)
				this.datastream = this.pid.substring(index + 1);
			this.pid =  this.pid.substring(0, index);
		}
	}

	public String getDatastream() {
		return datastream;
	}
	
	public String getDatastreamURI() {
		if (datastream == null)
			return getURI();
		return getURI() + "/" + datastream;
	}
}
