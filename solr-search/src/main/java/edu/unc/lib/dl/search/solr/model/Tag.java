package edu.unc.lib.dl.search.solr.model;

/**
 * @author count0
 *
 */
public class Tag {
	
	public Tag(String label, String text) {
		this.label = label;
		this.text =  text;
	}
	
	public Tag(String label, String text, String link, boolean emphasis) {
		this(label, text);
		this.link = link;
		this.emphasis = emphasis;
	}
	/**
	 * The tag label.
	 */
	private String label = null;
	
	/**
	 * The explanatory text for the tag.
	 */
	private String text = null;
	
	/**
	 * The link for the tag, if applicable. 
	 */
	private String link = null;
	
	/**
	 * Indicates that the tag has emphasis.
	 */
	private boolean emphasis = false;

	public boolean isEmphasis() {
		return emphasis;
	}
	public void setEmphasis(boolean emphasis) {
		this.emphasis = emphasis;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
}
