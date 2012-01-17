package edu.unc.lib.dl.cdr.services.sword;

public enum PackagingType {
	METS_CDR("http://cdr.unc.edu/METS/profiles/Simple"),
	METS_DSPACE_SIP("http://purl.org/net/sword/terms/METSDSpaceSIP"),
	SIMPLE_ZIP("http://purl.org/net/sword/terms/SimpleZip");
	
	private String uri;
	
	PackagingType(String uri){
		this.uri = uri;
	}
	
	public boolean equals(String value){
		return this.uri.equals(value);
	}

	@Override
	public String toString() {
		return this.uri;
	}
}
