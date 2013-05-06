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
package edu.unc.lib.dl.ui.view;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.util.ContentModelHelper;

public class HierarchicalTreeRecurseTag extends SimpleTagSupport {

	private HierarchicalBrowseResultResponse items;
	private PageContext pageContext;
	private JspWriter out;
	private JspFragment body;
	private boolean hideRoot = false;
	private Set<String> excludeIds = null;
	private String baseIndentCode;
	private String currentNodeVariableName;

	@Override
	public void doTag() throws JspException, IOException {
		this.pageContext = (PageContext) getJspContext();
		this.out = pageContext.getOut();
		this.body = getJspBody();
		
		if (items == null || items.getRootNode() == null)
			return;

		String leadupIndent = this.getLeadupIndent(this.baseIndentCode);

		renderNode(items.getRootNode(), leadupIndent, this.baseIndentCode, true, true);
	}

	private void renderNode(ResultNode currentNode, String leadupIndent, String indentCode, boolean firstEntry, boolean lastSibling) throws JspException, IOException {
		BriefObjectMetadata metadata = currentNode.getMetadata();
		// If this item is excluded, then skip it and its children.
		if (excludeIds != null && excludeIds.contains(metadata.getId())) {
			return;
		}
		
		// If this entry is a stub, then skip over it but render the children
		boolean isStub = metadata.getTitle() == null;
		
		String currentIndent;
		String nextGenerationIndent;
		String nextGenerationCode;
		if (firstEntry || isStub) {
			currentIndent = leadupIndent;
			nextGenerationIndent = leadupIndent;
			nextGenerationCode = indentCode;
		} else {
			currentIndent = leadupIndent + this.getIndent(true, true, lastSibling);
			nextGenerationIndent = leadupIndent + this.getIndent(!lastSibling, false, false);
			nextGenerationCode = indentCode + ((lastSibling)? '0' : '1');
		}
		
		out.println("<div class='entry_wrap' data-pid='" + metadata.getId() + "'>");
		
		if (!(this.hideRoot && firstEntry) && !isStub) {
			// Render the main entry, containing the contents from the jsp tag
			out.println("<div class='entry'>");
			out.println(currentIndent);

			pageContext.setAttribute("isRootNode", firstEntry);
			pageContext.setAttribute(this.currentNodeVariableName, currentNode);
			pageContext.setAttribute("indentCode", nextGenerationCode);
			pageContext.setAttribute("leadupIndent", leadupIndent);
			pageContext.setAttribute("lastSibling", lastSibling);
			
			body.invoke(out);

			// Ending the hier_entry tag
			out.println("</div>");
		}

		if ((currentNode.getChildren() != null && currentNode.getChildren().size() > 0)
				|| (metadata.getContentModel() != null && metadata.getContentModel().contains(ContentModelHelper.Model.CONTAINER.toString()))) {
			if (!isStub)
				out.println("<div id='children' " + metadata.getId().replace(':', '-') + "'>");

			if (currentNode.getChildren() != null) {
				for (int i = 0; i < currentNode.getChildren().size(); i++) {
					ResultNode childNode = currentNode.getChildren().get(i);
					this.renderNode(childNode, nextGenerationIndent, nextGenerationCode, firstEntry && isStub, i == currentNode.getChildren().size() - 1);
				}
			}

			if (!isStub)
				out.println("</div>");
		}
	}
	
	private String getLeadupIndent(String indentCode) {
		if (indentCode == null)
			return "";
		
		StringBuilder leadupIndent = new StringBuilder();
		char[] codeArray = indentCode.toCharArray();
		for (int i = 0; i < codeArray.length; i++) {
			leadupIndent.append(getIndent(codeArray[i] == '1', false, false));
		}
		return leadupIndent.toString();
	}
	
	private String getIndent(boolean occupiedIndent, boolean lastTier, boolean lastSibling) {
		if (!occupiedIndent) {
			return "<div class=\"indent_unit\"></div>";
		}
		if (lastTier) {
			if (lastSibling) {
				return "<div class=\"indent_unit hier_container\"></div>";
			} else {
				return "<div class=\"indent_unit hier_container hier_with_siblings\"></div>";
			}
		}
		return "<div class=\"indent_unit hier_with_siblings\"></div>";
	}

	public void setItems(HierarchicalBrowseResultResponse items) {
		this.items = items;
	}

	public void setHideRoot(boolean hideRoot) {
		this.hideRoot = hideRoot;
	}

	public void setExcludeIds(Set<String> excludeIds) {
		this.excludeIds = excludeIds;
	}
	
	public void setVar(String var) {
		this.currentNodeVariableName = var;
	}
	
	public void setExcludeIds(String excludeIds) {
		String[] excludeArray = excludeIds.split(" ");
		this.excludeIds = new HashSet<String>();
		
		for (String excludeId: excludeArray) {
			this.excludeIds.add(excludeId);
		}
	}

	public void setBaseIndentCode(String baseIndentCode) {
		this.baseIndentCode = baseIndentCode;
	}
}
