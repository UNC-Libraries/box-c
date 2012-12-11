package edu.unc.lib.dl.search.solr.model;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.DatastreamCategory;

public class Datastream {
	private PID owner;
	private String name;
	private Long filesize;
	private String mimetype;
	private String extension;
	private String checksum;
	private ContentModelHelper.Datastream datastreamClass;

	public Datastream(String datastream) {
		if (datastream == null)
			throw new IllegalArgumentException("Datastream value must not be null");
		
		String[] dsParts = datastream.split("\\|");
		
		this.name = dsParts[0];
		
		if (dsParts.length < 2)
			return;
		
		this.mimetype = dsParts[1];
		this.extension = dsParts[2];
		try {
			this.filesize = new Long(dsParts[3]);
		} catch (NumberFormatException e) {
			this.filesize = new Long(0);
		}
		if (dsParts.length > 4 && dsParts[4].length() > 0)
			this.checksum = dsParts[4];
		else this.checksum = null;
		if (dsParts.length > 5 && dsParts[5].length() > 0) {
			this.owner = new PID(dsParts[5]);
		} else {
			this.owner = null;
		}
	}
	
	public String toString() {
		//DS name|mimetype|extension|filesize|checksum|owner
		StringBuilder sb = new StringBuilder(name);
		sb.append('|').append(mimetype).append('|');
		if (extension != null)
			sb.append(extension);
		sb.append('|').append(filesize).append('|');
		if (this.checksum != null)
			sb.append(checksum);
		sb.append('|');
		if (owner != null)
			sb.append(owner.getPid());
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == null)
			return false;
		if (object instanceof Datastream) {
			Datastream rightHand = (Datastream)object;
			// Equal if names match and either pids are null or both match
			return name.equals(rightHand.name) && (rightHand.owner == null || owner == null || owner.equals(rightHand.owner));
		}
		if (object instanceof String) {
			String rightHandString = (String)object;
			if (rightHandString.equals(this.name))
				return true;
			Datastream rightHand = new Datastream(rightHandString);
			return this.equals(rightHand);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	public String getDatastreamIdentifier() {
		if (owner == null)
			return name;
		return owner.getPid() + "/" + name;
	}

	public String getName() {
		return name;
	}

	public PID getOwner() {
		return owner;
	}

	public Long getFilesize() {
		return filesize;
	}

	public String getMimetype() {
		return mimetype;
	}

	public String getExtension() {
		return extension;
	}

	public String getChecksum() {
		return checksum;
	}
	
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public void setOwner(PID owner) {
		this.owner = owner;
	}

	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}

	public DatastreamCategory getDatastreamCategory() {
		if (datastreamClass == null) {
			this.datastreamClass = ContentModelHelper.Datastream.getDatastream(this.name);
		}
		if (datastreamClass == null) {
			System.out.println("No datastream class available for " + this.name);
			return null;
		}
		System.out.println("Datastream class for " + this.name);
		return datastreamClass.getCategory();
	}
}
