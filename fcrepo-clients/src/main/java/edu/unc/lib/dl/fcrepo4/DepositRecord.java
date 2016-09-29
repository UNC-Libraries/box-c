package edu.unc.lib.dl.fcrepo4;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

public class DepositRecord {

	private String path;
	private Repository repository;
	
	public DepositRecord(String path, Repository repository) {
		this.repository = repository;
		this.path = path;
	}
	
	/**
	 * Adds the given file as a manifest for this deposit. 
	 * 
	 * @param manifest
	 * @return path to the newly created manifest object
	 */
	public String addManifest(File manifest) {
		return null;
	}
	
	public InputStream getManifest() {
		return null;
	}
	
	public Collection<String> getManifestPaths() {
		return null;
	}
	
	public Collection<?> listDepositedObjects() {
		return null;
	}

}
