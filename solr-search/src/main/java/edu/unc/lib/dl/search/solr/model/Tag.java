/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
