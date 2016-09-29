package edu.unc.lib.dl.fcrepo4;

import edu.unc.lib.dl.fedora.PID;

public class ContentObject {
	private String fedoraUri;
	private PCDMObject pcdm;
	private Repository repository;
	
	public ContentObject(String path, Repository repository) {
		fedoraUri = path;
		this.repository = repository;
	}
	
	public String getChildPath(PID pid) {
		return "";
	}
	
}
