package edu.unc.lib.dl.search.solr.util;

public enum ContentCategory {
	dataset("Dataset"), image("Image"), diskimage("Disk Image"), video("Video"), software("Software"),
	audio("Audio"), archive("Archive File"), text("Text"), unknown("Unknown");
	
	private String displayName;
	private String joined;
	
	ContentCategory(String displayName) {
		this.displayName = displayName;
		this.joined = this.name() + "," + this.displayName;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}
	
	public String getJoined() {
		return joined;
	}
	
	public static ContentCategory getContentCategory(String name) {
		if (name == null)
			return unknown;
		for (ContentCategory category: values())
			if (category.name().equals(name))
				return category;
		return unknown;
	}
}