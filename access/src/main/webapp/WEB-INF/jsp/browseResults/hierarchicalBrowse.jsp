<%--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> 
<%--
	Renders a hierarchical browse view of all the children containers starting at a given root node.

	hierarchicalViewResults - HierarchicalBrowseResultResponse object which contains the result set.  Required.
	rootNode - The base container, all other items will be indented relative to it.  From request scope.
	queryPath - override the servlet that facet links will send the user to.  Default "search"
	displayCounts - Boolean, indicates if child counts should be displayed next to contain names.  From param scope.
	ignoreSearchState - If true, then preexisting search state parameters will not be included in container urls. 
	applyCutoffs - Determines if hierarchical facet cutoffs are appended added facets when selecting containers
	displaySecondaryActions - whether or not ot show the secondary actions for each container
--%>
<c:set var="rootNode" value="${hierarchicalViewResults.resultList[0]}"/>
<c:set var="baseTier" scope="page" value="${rootNode.path.highestTier}"/> 

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.applyCutoffs}"><c:set var="applyCutoffs" value="${param.applyCutoffs}"/></c:when>
	<c:otherwise><c:set var="applyCutoffs" value="true"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.applyCountLinkCutoffs}"><c:set var="applyCountLinkCutoffs" value="${param.applyCountLinkCutoffs}"/></c:when>
	<c:otherwise><c:set var="applyCountLinkCutoffs" value="true"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.filePrimaryDownload}"><c:set var="filePrimaryDownload" value="${param.filePrimaryDownload}"/></c:when>
	<c:otherwise><c:set var="filePrimaryDownload" value="false"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.showSeeAllLinks}"><c:set var="showSeeAllLinks" value="${param.showSeeAllLinks}"/></c:when>
	<c:otherwise><c:set var="showSeeAllLinks" value="true"/></c:otherwise>
</c:choose>

<c:if test="${not empty param.countLinkPath}">
	<c:set var="countLinkUrlBase" value="${param.countLinkPath}?${sessionScope.recordNavigationState.searchStateUrl}"/>
</c:if>

<c:choose>
	<c:when test="${not empty param.excludeIDs}"><c:set var="excludeIDs" value="${param.excludeIDs}"/></c:when>
	<c:otherwise><c:set var="excludeIDs" value=""/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${empty searchStateUrl || param.ignoreSearchState == true}">
		<c:set var="containerUrlBase" value=""/>
		<c:if test="${not empty param.countLinkPath}">
			<c:set var="countLinkUrlBase" value="${param.countLinkPath}"/>
		</c:if>
	</c:when>
	<c:otherwise>
		<c:set var="containerUrlBase" value="?${sessionScope.recordNavigationState.searchStateUrl}"/>
		<c:if test="${not empty param.countLinkPath}">
			<c:set var="countLinkUrlBase" value="${param.countLinkPath}?${searchStateUrl}"/>
		</c:if>
	</c:otherwise>
</c:choose>
<%!
String getIndentTag(char indentBit) {
	if (indentBit == '1') {
		return "<div class=\"indent_unit hier_with_siblings\"></div>";
	}
	return "<div class=\"indent_unit\"></div>";
}

String generateLeadupIndent(String indentCode) {
	StringBuilder indent = new StringBuilder();
	for (char indentBit: indentCode.toCharArray()) {
		indent.append(getIndentTag(indentBit));
	}
	return indent.toString();
}

void decrementLongMap(java.util.Map<String,Long> map, String key){
	if (!map.containsKey(key))
		return;
	map.put(key, map.get(key) - 1);
}

void renderEntry(HttpServletRequest request, PageContext pageContext, boolean isFirst) throws Exception {
	edu.unc.lib.dl.search.solr.util.SearchSettings searchSettings = (edu.unc.lib.dl.search.solr.util.SearchSettings)pageContext.getAttribute("searchSettings");
	edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse hierarchicalViewResults = (edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse)pageContext.getAttribute("hierarchicalViewResults");
	edu.unc.lib.dl.search.solr.model.BriefObjectMetadata containerNode = (edu.unc.lib.dl.search.solr.model.BriefObjectMetadata)pageContext.getAttribute("containerNode");
	edu.unc.lib.dl.search.solr.model.BriefObjectMetadata rootNode = (edu.unc.lib.dl.search.solr.model.BriefObjectMetadata)pageContext.getAttribute("rootNode");
	long childCount = (containerNode.getCountMap() != null && containerNode.getCountMap().size() > 0)? containerNode.getCountMap().get("child") : 0L;
	boolean applyCutoffs = Boolean.parseBoolean((String)pageContext.getAttribute("applyCutoffs"));
	boolean excludeParent = Boolean.parseBoolean((String)request.getAttribute("excludeParent"));
	String queryPath = (String)pageContext.getAttribute("queryPath");
	String containerUrlBase = (String)pageContext.getAttribute("containerUrlBase");
	String searchStateUrl = (String)request.getAttribute("searchStateUrl");
	boolean hideTypeIcon = Boolean.parseBoolean(request.getParameter("hideTypeIcon"));
	
	boolean isContainer = containerNode.getContentModel().contains(edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER.toString());
	boolean isAggregate = searchSettings.getResourceTypeAggregate().equals(containerNode.getResourceType());
	boolean isCollection = searchSettings.getResourceTypeCollection().equals(containerNode.getResourceType());
	
	StringBuilder endContainerDivs = new StringBuilder();
	
	Long subContainerCount = hierarchicalViewResults.getSubcontainerCounts().get(containerNode.getPath().getSearchValue());
	if (subContainerCount == null)
		subContainerCount = 0L;
	
	boolean retainedAsDirectMatch = hierarchicalViewResults.getMatchingContainerPids().size() > 0 
			&& (childCount == 0 || subContainerCount == childCount);
	
	// Determine the action that occurs when the container is clicked on
	String containerFacetAction;
	if (!isContainer) {
		containerFacetAction = searchSettings.getActionName("SET_FACET") + ":" + searchSettings.getSearchFieldParams().get("ANCESTOR_PATH") + ",\"" 
			+ containerNode.getAncestorPathFacet().getSearchValue() + "," + (containerNode.getAncestorPathFacet().getHighestTier() + 1) +"\""; 
	} else if (applyCutoffs) {
		containerFacetAction = searchSettings.getActionName("SET_FACET") + ":" + searchSettings.getSearchFieldParams().get("ANCESTOR_PATH") + ",\"" 
				+ containerNode.getPath().getLimitToValue() + "\"";
	} else {
		containerFacetAction = searchSettings.getActionName("SET_FACET") + ":" + searchSettings.getSearchFieldParams().get("ANCESTOR_PATH") + ",\"" 
				+ containerNode.getPath().getSearchValue() + "\"";
	}
	
	// Build the URL that performs the action of clicking on the container
	String containerUrl = queryPath;
	if (!(retainedAsDirectMatch && "search".equals(queryPath))) {
		containerUrl += containerUrlBase + "&";
	} else if (!containerUrl.contains("?")){
		containerUrl += "?";
	}
	containerUrl += searchSettings.getSearchStateParams().get("ACTIONS") + "=" + org.springframework.web.util.UriUtils.encodeQueryParam(containerFacetAction, "UTF-8");
	
	// Construct the primary URL for this container (link for the title) and the tooltip for it
	String primaryUrl, primaryTooltip;
	if ("search".equals(queryPath)) {
		if (retainedAsDirectMatch) {
			primaryUrl = "/search?";
			primaryTooltip = "View all items contained within" + containerNode.getResourceType().toLowerCase() + "&nbsp;" + containerNode.getTitle();
		} else {
			primaryUrl = "/search" + containerUrlBase + "&";
			primaryTooltip = "View " + childCount + " matching item(s) contained within " + containerNode.getResourceType().toLowerCase() + "&nbsp;" + containerNode.getTitle();
		}
		primaryUrl += searchSettings.getSearchStateParams().get("ACTIONS") + "=" + org.springframework.web.util.UriUtils.encodeQueryParam(
				containerFacetAction + "|" + searchSettings.getActionName("RESET_NAVIGATION"), "UTF-8");
	} else {
		primaryUrl = "/browse" + containerUrlBase + "&";
		primaryUrl += searchSettings.getSearchStateParams().get("ACTIONS") + "=" + org.springframework.web.util.UriUtils.encodeQueryParam(
				containerFacetAction, "UTF-8");
		primaryTooltip = "Browse structure starting from " + containerNode.getResourceType().toLowerCase() + "&nbsp;" + containerNode.getTitle();
	}
	
	String viewAllResultsLink = "";
	
	// Generate the baseline indentation for this record
	String indentCode;
	String defaultIndent = request.getParameter("indentCode");
	if (defaultIndent != null)
		indentCode = defaultIndent;
	else indentCode = "";
	
	String leadupIndent = generateLeadupIndent(indentCode);
	boolean hasSubcontainers = subContainerCount > 0;
	
	long precedingTierCount;
	StringBuilder entryOut = new StringBuilder();
	if (!isFirst) {
		int baseTier = Integer.parseInt((String)pageContext.getAttribute("baseTier"));
		precedingTierCount = hierarchicalViewResults.getSubcontainerCounts().get(rootNode.getPath().getSearchValue()) - 1;
		for (int i = 0; i < containerNode.getPath().getFacetNodes().size(); i++) {
			edu.unc.lib.dl.search.solr.model.CutoffFacetNode pathNode = 
					(edu.unc.lib.dl.search.solr.model.CutoffFacetNode)containerNode.getPath().getFacetNodes().get(i);
			if (pathNode.getTier() < baseTier)
				continue;
			
			// If this node is the last node and there are no more subcontainers, then the entry is closed
			if (i == (containerNode.getPath().getFacetNodes().size() - 1) && 
					(precedingTierCount == subContainerCount || (subContainerCount != null && precedingTierCount == 0))) {
				indentCode += 0;
				entryOut.append(leadupIndent).append("<div class=\"indent_unit hier_container\"></div>");
			} else if (i == (containerNode.getPath().getFacetNodes().size() - 1) && 
					(precedingTierCount > 0 && (subContainerCount != null || precedingTierCount > subContainerCount))) {
				// Last node with more subcontainers gets displayed as having siblings
				indentCode += 1;
				entryOut.append(leadupIndent).append("<div class=\"indent_unit hier_container hier_with_siblings\"></div>");
			} else if (precedingTierCount >= subContainerCount) {
				// Intermediate entry with siblings
				indentCode += 1;
				leadupIndent += "<div class=\"indent_unit hier_with_siblings\"></div>";
				decrementLongMap(hierarchicalViewResults.getSubcontainerCounts(), pathNode.getSearchValue());
				subContainerCount--;
			} else if (pathNode.getTier() == baseTier) {
				decrementLongMap(hierarchicalViewResults.getSubcontainerCounts(), pathNode.getSearchValue());
				subContainerCount--;
			} else {
				indentCode += 0;
				entryOut.append(leadupIndent).append("<div class=\"indent_unit\"></div>");
				decrementLongMap(hierarchicalViewResults.getSubcontainerCounts(), pathNode.getSearchValue());
				subContainerCount--;
			}
			
			// No more children reported for this item, so close it off
			if (subContainerCount <= 0L) {
				if (!isContainer && i == containerNode.getAncestorPathFacet().getHighestTier()) {
					viewAllResultsLink = "<div class='hier_entry'>" + leadupIndent + "<a href=\"" + containerUrl + "\">(see all)</a></div>";	
				}
				
				if ((pathNode.getTier() == baseTier && excludeParent) && isContainer) {
					endContainerDivs.append("</div>");
				}
			}
			
			precedingTierCount = subContainerCount;
		}
	}
	boolean firstEntryBrowseSelected = ("browse".equals(queryPath) && childCount == 0 || (isFirst 
			&& hierarchicalViewResults.getSearchState().getFacets().containsKey("ANCESTOR_PATH")));
	
	if (isContainer) {
		// Determine whether to display a collapse or expand icon
		if (subContainerCount == 0 && retainedAsDirectMatch) {
			entryOut.append("<img src=\"/static/images/no_action.png\"/>");
		} else if (subContainerCount > 0 && ((isFirst && !"browse".equals(queryPath)) || hierarchicalViewResults.getSearchState().getRowsPerPage() == 0)) {
			entryOut.append("<a href=\"#\" class=\"hier_action collapse\" title=\"Collapse " + containerNode.getTitle() + "\""); 
			entryOut.append("id=\"container_toggle_" + containerNode.getId().replace(':', '-') + "\"><img src=\"/static/images/collapse.png\"/></a>");
		} else if (!hasSubcontainers && childCount == 0) {
			entryOut.append("<a class=\"hier_action\" title=\"" + containerNode.getTitle() + " is empty\"><img src=\"/static/images/no_action.png\"/></a>");
		} else {
			// Otherwise its an expand action
			String actions = searchSettings.getActions().get("SET_FACET") + ":" + searchSettings.getSearchFieldParams().get("ANCESTOR_PATH") +",\"" + containerNode.getPath().getSearchValue() + "\"";
			if (hasSubcontainers) {
				actions += searchSettings.getActions().get("SET_RESOURCE_TYPE") + ":" + searchSettings.getResourceTypeFile();
			}
			actions = org.springframework.web.util.UriUtils.encodeQueryParam(actions, "UTF-8");
			
			StringBuilder expandUrl = new StringBuilder();
			expandUrl.append("#browse?").append(searchStateUrl).append("&depth=1&indentCode=").append(indentCode).append("&ajax=true");
			expandUrl.append('&').append(searchSettings.getSearchStateParams().get("ACTIONS")).append('=').append(actions);
			expandUrl.append('&').append("disableSecondaryDetailsLink").append('=').append(request.getParameter("disableSecondaryDetailsLink"));
			expandUrl.append('&').append("hideTypeIcon").append('=').append(request.getParameter("hideTypeIcon"));
			
			entryOut.append("<a href=\"" + expandUrl + "\" title=\"Expand " + containerNode.getTitle() + "\"");
			entryOut.append("class=\"hier_action expand hier_container_not_loaded\" id=\"container_toggle_" + containerNode.getId().replace(':', '-') + "\">");
			entryOut.append("<img src=\"/static/images/expand.png\"/></a>");
		}			
	}
	
	if (!hideTypeIcon) {
		if (isCollection) {
			entryOut.append("<img src=\"/static/images/hier_collection.png\" alt=\"Collection\" title=\"Collection\" class=\"resource_type\"/>");
		} else if (isAggregate || !isContainer) {
			entryOut.append("<img src=\"/static/images/hier_file.png\" alt=\"File\" title=\"File\" class=\"resource_type\"/>");
		} else {
			entryOut.append("<img src=\"/static/images/hier_folder.png\" alt=\"Folder\" title=\"Folder\" class=\"resource_type\"/>");
		}
	}
	
	entryOut.append("<div class=\"hier_entry_description\">");
	if (firstEntryBrowseSelected) {
		entryOut.append("<a class=\"hier_entry_primary_action\" title=\"Currently browsing " + containerNode.getTitle() + "\">")
		.append(containerNode.getTitle()).append("</a>");
	} else if (isAggregate || !isContainer) {
		entryOut.append("<a href=\"/record?id=").append(containerNode.getId()).append("\" class=\"hier_entry_primary_action\" title=\"View details for this item.\">")
				.append(containerNode.getTitle()).append("</a>");
	} else {
		entryOut.append("<a href=\"").append(primaryUrl).append("\" class=\"hier_entry_primary_action\" title=\"" + primaryTooltip + "\">")
				.append(containerNode.getTitle()).append("</a>");
	}
	
	boolean displayCounts = Boolean.parseBoolean(request.getParameter("displayCounts"));
	boolean displaySecondaryActions = Boolean.parseBoolean(request.getParameter("displaySecondaryActions"));
	if (isContainer) {
		if (displayCounts && !(retainedAsDirectMatch && childCount == 0)) {
			entryOut.append("<span class=\"hier_count\">(").append(childCount).append(")</span>");
		}
	} else if (displaySecondaryActions){
		entryOut.append("<p class=\"hier_secondary_actions\">");
		if (edu.unc.lib.dl.ui.util.AccessUtil.permitDatastreamAccess(edu.unc.lib.dl.acl.util.GroupsThreadStore.getGroups(), "DATA_FILE", containerNode)) {
			edu.unc.lib.dl.ui.util.FedoraUtil fedoraUtil = (edu.unc.lib.dl.ui.util.FedoraUtil)pageContext.getAttribute("fedoraUtil");
			entryOut.append("<a href=\"").append(fedoraUtil.getDatastreamUrl(containerNode, "DATA_FILE")).append("&dl=true\">Download</a>");
		}
	}
		
	
	/*
	<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						<c:if test="${param.displaySecondaryActions}">
							<p class="hier_secondary_actions">
								<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', containerNode)}">
									<c:if test="${!filePrimaryDownload}">
										<a href="${cdr:getDatastreamUrl(containerNode, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
									</c:if>
									(<c:out value="${containerNode.contentTypeFacet[0].displayValue}"/> 
									<c:if test="${not empty containerNode.filesizeSort}">
										&nbsp;<c:out value="${cdr:formatFilesize(containerNode.filesizeSort, 1)}"/>
									</c:if>)
								</c:if>
							</p>
						</c:if>
					</c:when>
					<c:otherwise>
						<c:if test="${param.displayCounts && !(retainedAsDirectMatch && childCount == 0)}">
							<span class="hier_count">(${childCount})</span>
						</c:if>
						
						<c:if test="${param.displaySecondaryActions}">
							<c:url var="secondaryBrowseUrl" scope="page" value='browse${containerUrlBase}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue}"'/>
							</c:url>
							
							<c:url var="secondarySearchWithStateUrl" scope="page" value='search?${sessionScope.recordNavigationState.searchStateUrl}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
							</c:url>
							
							<c:url var="secondarySearchPathUrl" scope="page" value='search'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
							</c:url>
					
							<p class="hier_secondary_actions">
								<c:if test="${!param.disableSecondaryBrowseLink && !firstEntryBrowseSelected}">
									<a href="<c:out value="${secondaryBrowseUrl}" />" class="hier_entry_secondary_action" title="Browse structure starting from ${fn:toLowerCase(containerNode.resourceType)} <i>${containerNode.title}</i>">Structure</a>&nbsp;
								</c:if>
								<c:if test="${!param.disableSecondarySearchWithStateLink}">
									<c:choose>
										<c:when test="${retainedAsDirectMatch}">
											(<a href="<c:out value="${secondarySearchPathUrl}" />">View contents</a>)&nbsp;
										</c:when>
										<c:otherwise>
											(<a href="<c:out value="${secondarySearchWithStateUrl}" />">View contents</a>)&nbsp;
										</c:otherwise>
									</c:choose>
								</c:if>
								<c:if test="${!param.disableSecondarySearchPathLink}">
									(<a href="<c:out value="${secondarySearchPathUrl}" />">View all contents</a>)&nbsp;
								</c:if>
								<c:if test="${!param.disableSecondaryDetailsLink}">
									<a href="record?id=${containerNode.id}" class="hier_entry_secondary_action" title="View ${fn:toLowerCase(containerNode.resourceType)} information for ${containerNode.title}">Details</a>
								</c:if>
							</p>
						</c:if>
					</c:otherwise>
				</c:choose>
	*/
}
%>
<c:forEach items="${hierarchicalViewResults.resultList}" var="containerNode" varStatus="resultStatus">
	<c:if test="${!(param.excludeParent && resultStatus.first)}">
		<c:choose>
			<c:when test="${not empty containerNode.countMap}">
				<c:set var="childCount" value="${containerNode.countMap.child}"/>
			</c:when>
			<c:otherwise>
				<c:set var="childCount" value="0"/>
			</c:otherwise>
		</c:choose>
		
		<c:set var="endContainerDivs" value="" />
		<c:set var="indentCode" value="" />
		<c:set var="entryOut" value="" />
		
		<c:set var="retainedAsDirectMatch" value="${fn:length(hierarchicalViewResults.matchingContainerPids) > 0
					&& (childCount == 0
					|| hierarchicalViewResults.subcontainerCounts[containerNode.path.facetNodes[fn:length(containerNode.path.facetNodes) - 1].searchValue] == childCount)}"/>
		
		<c:set var="containerFacetAction">
			<c:choose>
				<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
					${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams['ANCESTOR_PATH']},"${containerNode.ancestorPathFacet.searchValue},${containerNode.ancestorPathFacet.highestTier + 1}"
				</c:when>
				<c:when test="${applyCutoffs}">
					${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams['ANCESTOR_PATH']},"${containerNode.path.limitToValue}"
				</c:when>
				<c:otherwise>
					${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams['ANCESTOR_PATH']},"${containerNode.path.searchValue}"
				</c:otherwise>
			</c:choose>
		</c:set>
		
		<c:choose>
			<c:when test="${retainedAsDirectMatch && queryPath == 'search'}">
				<%-- If the item has no children but was returned as a search result,  --%>
				<c:url var="containerUrl" scope="page" value='${queryPath}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${containerFacetAction}"/>
				</c:url>
			</c:when>
			<c:otherwise>
				<c:url var="containerUrl" scope="page" value='${queryPath}${containerUrlBase}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${containerFacetAction}"/>
				</c:url>			
			</c:otherwise>
		</c:choose>
		
		<c:choose>
			<c:when test="${queryPath == 'search' && retainedAsDirectMatch}">
				<c:url var="primaryUrl" scope="page" value='search'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${containerFacetAction}|${searchSettings.actions['RESET_NAVIGATION']}:search"/>
				</c:url>
				<c:set var="primaryTooltip">View all items contained within ${fn:toLowerCase(containerNode.resourceType)}&nbsp;${containerNode.title}</c:set>
			</c:when>
			<c:when test="${queryPath == 'search' && !retainedAsDirectMatch}">
				<c:url var="primaryUrl" scope="page" value='search${containerUrlBase}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${containerFacetAction}|${searchSettings.actions['RESET_NAVIGATION']}:search"/>
				</c:url>
				<c:set var="primaryTooltip">View ${childCount} matching item(s) contained within ${fn:toLowerCase(containerNode.resourceType)}&nbsp;${containerNode.title}</c:set>
			</c:when>
			<c:otherwise>
				<c:url var="primaryUrl" scope="page" value='browse${containerUrlBase}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${containerFacetAction}"/>
				</c:url>
				<c:set var="primaryTooltip">Browse structure starting from ${fn:toLowerCase(containerNode.resourceType)}&nbsp;${containerNode.title}</c:set>
			</c:otherwise>
		</c:choose>
		
		<c:set var="viewAllResultsLink" value=""/>
		<c:set var="leadupIndent" value="" scope="page"/>
		<c:if test="${not empty param.indentCode}">
			<c:set var="indentCode" value="<c:out value='${param.indentCode}'/>${indentCode}"/>
			<c:forEach var="i" begin="0" end="${fn:length(param.indentCode)}" step="1">
				<c:choose>
					<c:when test="${fn:substring(param.indentCode, i, i + 1) == '1'}">
						<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit hier_with_siblings"></div>'/>
					</c:when>
					<c:when test="${fn:substring(param.indentCode, i, i + 1) == '0'}">
						<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit"></div>'/>
					</c:when>
				</c:choose>     
			</c:forEach>
		</c:if>
		<c:set var="hasSubcontainers" value="${hierarchicalViewResults.subcontainerCounts[containerNode.path.facetNodes[fn:length(containerNode.path.facetNodes) - 1].searchValue] > 0}" />
		<%-- Show the next entry unless its the Collections object --%>
		<c:if test="${!resultStatus.first}">
			<c:set var="precedingTierCount" value="${hierarchicalViewResults.subcontainerCounts[rootNode.path.facetNodes[fn:length(rootNode.path.facetNodes) - 1].searchValue] - 1}"/>
			<%-- Step through the current items ancestor path to figure out and render its indentation --%>
			<c:forEach var="pathNode" items="${containerNode.path.facetNodes}" varStatus="status">
				<c:if test="${pathNode.tier >= baseTier}">
					<c:set var="subcontainerCount" value="${hierarchicalViewResults.subcontainerCounts[pathNode.searchValue]}"/>
					<c:choose>
						<c:when test="${status.last && (precedingTierCount == subcontainerCount 
								|| (empty subcontainerCount && precedingTierCount == 0))}">
							<c:set var="indentCode" value="${indentCode}0"/>
							
							<c:set var="entryOut">${entryOut}${leadupIndent}<div class="indent_unit hier_container"></div></c:set>
						</c:when>
						<c:when test="${status.last && precedingTierCount > 0 && 
								(empty subcontainerCount || precedingTierCount > subcontainerCount)}">
							<c:set var="indentCode" value="${indentCode}1"/>
							<c:set var="entryOut">${entryOut}${leadupIndent}<div class="indent_unit hier_container hier_with_siblings"></div></c:set>
						</c:when>
						<c:when test="${precedingTierCount >= hierarchicalViewResults.subcontainerCounts[pathNode.searchValue]}">
							<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit hier_with_siblings"></div>'/>
							<c:set var="indentCode" value="${indentCode}1"/>
							${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathNode.searchValue) }
						</c:when>
						<c:when test="${pathNode.tier == baseTier && param.excludeParent}">
							${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathNode.searchValue) }
						</c:when>
						<c:when test="${pathNode.tier == baseTier}">
							${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathNode.searchValue) }
						</c:when>
						<c:otherwise>
							<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit"></div>'/>
							${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathNode.searchValue) }
							<c:set var="indentCode" value="${indentCode}0"/>
						</c:otherwise>
					</c:choose>
					<%-- No children reported for this item, so finish it off --%>
					<c:if test="${empty hierarchicalViewResults.subcontainerCounts[pathNode.searchValue] 
							|| hierarchicalViewResults.subcontainerCounts[pathNode.searchValue] <= 0}">
						ending ${pathNode.displayValue}|
						<c:if test="${containerNode.resourceType == searchSettings.resourceTypeFile
								&& status.count == containerNode.ancestorPathFacet.highestTier}">
							<c:set var="viewAllResultsLink">
								<div class="hier_entry">
									${leadupIndent}
									<a href="<c:out value='${containerUrl}' />">(see all)</a>
								</div>
							</c:set>
						</c:if>
						<%-- Ended a container, so add a closing div --%>
						<c:if test="${!(pathNode.tier == baseTier && param.excludeParent) 
										&& (containerNode.resourceType != searchSettings.resourceTypeFile)}">
							<c:set var="endContainerDivs" value="${endContainerDivs}</div>"/>
						</c:if>
						
					</c:if>
					<%-- Shows sub container counts by depth --%>
					<%-- <div style="float: right;">&nbsp;&nbsp;(${precedingTierCount}|${subcontainerCount})</div> --%>
					<c:set var="precedingTierCount" value="${hierarchicalViewResults.subcontainerCounts[pathNode.searchValue]}"/>
				</c:if>
			</c:forEach>
		</c:if>
		
		<c:set var="firstEntryBrowseSelected" value="${queryPath == 'browse' && (childCount == 0 ||
				(resultStatus.first && not empty hierarchicalViewResults.searchState.facets['ANCESTOR_PATH']))}"/>
		
		<c:set var="entryOut">${entryOut}
			<c:if test="${containerNode.resourceType != searchSettings.resourceTypeFile}">
				<c:set var="subcontainerCount" value="${hierarchicalViewResults.subcontainerCounts[containerNode.path.facetNodes[fn:length(containerNode.path.facetNodes) - 1].searchValue]}" scope="page"/>
				<%-- Determine whether to display a collapse or expand icon --%>
				<c:choose>
					<c:when test="${(empty subcontainerCount || subcontainerCount == 0) && retainedAsDirectMatch}">
						<img src="/static/images/no_action.png"/>
					</c:when>
					<c:when test="${subcontainerCount > 0 &&
							((resultStatus.first && queryPath != 'browse') ||
							hierarchicalViewResults.searchState.rowsPerPage == 0)}">
						<a href="#" class="hier_action collapse" title="Collapse ${containerNode.title}" id="container_toggle_${fn:replace(containerNode.id, ':', '-')}"><img src="/static/images/collapse.png"/></a>
					</c:when>
					<c:when test="${!hasSubcontainers && childCount == 0}">
						<a class="hier_action" title="${containerNode.title} is empty"><img src="/static/images/no_action.png"/></a>
					</c:when>
					<c:otherwise>
						<c:set var="actions" scope="page" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue}"'/>
						<c:if test="${hasSubcontainers}">
							<c:set var="actions" value='${actions}|${searchSettings.actions["SET_RESOURCE_TYPE"]}:${searchSettings.resourceTypeFile}'/>
						</c:if>
						<c:url var="expandUrl" value="#browse?${searchStateUrl}&depth=1&indentCode=${indentCode}&ajax=true">
							<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${actions}'/>
							<c:param name="disableSecondaryDetailsLink" value='${param.disableSecondaryDetailsLink}'/>
							<c:param name="hideTypeIcon" value='${param.hideTypeIcon}'/>
						</c:url>
						<a href="<c:out value='${expandUrl}' />" title="Expand ${containerNode.title}" class="hier_action expand hier_container_not_loaded" id="container_toggle_${fn:replace(containerNode.id, ':', '-')}"><img src="/static/images/expand.png"/></a>
					</c:otherwise>
				</c:choose>
			</c:if>
		
			<c:if test="${param.hideTypeIcon == false }">
				<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeCollection}">
						<img src="/static/images/hier_collection.png" alt="Collection" title="Collection" class="resource_type"/>
					</c:when>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile || containerNode.resourceType == searchSettings.resourceTypeAggregate}">
						<img src="/static/images/hier_file.png" alt="File" title="File" class="resource_type"/>
					</c:when>
					<c:otherwise>
						<img src="/static/images/hier_folder.png" alt="Folder" title="Folder" class="resource_type"/>
					</c:otherwise>
				</c:choose>
			</c:if>
			
			<div class="hier_entry_description">
				<c:choose>
					<c:when test="${firstEntryBrowseSelected}">
						<a class="hier_entry_primary_action" title="Currently browsing ${containerNode.title}">${containerNode.title}</a>
					</c:when>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile || containerNode.resourceType == searchSettings.resourceTypeAggregate}">
						<c:choose>
							<c:when test="${filePrimaryDownload}">
								<c:url var="filePrimaryUrl" scope="page" value="${cdr:getDatastreamUrl(containerNode, 'DATA_FILE', fedoraUtil)}&dl=true"/>
								<a href="<c:out value='${filePrimaryUrl}' />" class="hier_entry_primary_action" title="Download this item.">${containerNode.title}</a>
							</c:when>
							<c:otherwise>
								<c:url var="filePrimaryUrl" value="record">
									<c:param name="id" value="${containerNode.id}"/>
								</c:url>
								<a href="<c:out value='${filePrimaryUrl}' />" class="hier_entry_primary_action" title="View details for this item.">${containerNode.title}</a>
							</c:otherwise>
						</c:choose>
					</c:when>
					<c:otherwise>
						<a href="<c:out value='${primaryUrl}' />" class="hier_entry_primary_action" title="${primaryTooltip}">${containerNode.title}</a>
					</c:otherwise>
				</c:choose>
				
				<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						<c:if test="${param.displaySecondaryActions}">
							<p class="hier_secondary_actions">
								<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', containerNode)}">
									<c:if test="${!filePrimaryDownload}">
										<a href="${cdr:getDatastreamUrl(containerNode, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
									</c:if>
									(<c:out value="${containerNode.contentTypeFacet[0].displayValue}"/> 
									<c:if test="${not empty containerNode.filesizeSort}">
										&nbsp;<c:out value="${cdr:formatFilesize(containerNode.filesizeSort, 1)}"/>
									</c:if>)
								</c:if>
							</p>
						</c:if>
					</c:when>
					<c:otherwise>
						<c:if test="${param.displayCounts && !(retainedAsDirectMatch && childCount == 0)}">
							<span class="hier_count">(${childCount})</span>
						</c:if>
						
						<c:if test="${param.displaySecondaryActions}">
							<c:url var="secondaryBrowseUrl" scope="page" value='browse${containerUrlBase}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue}"'/>
							</c:url>
							
							<c:url var="secondarySearchWithStateUrl" scope="page" value='search?${sessionScope.recordNavigationState.searchStateUrl}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
							</c:url>
							
							<c:url var="secondarySearchPathUrl" scope="page" value='search'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
							</c:url>
					
							<p class="hier_secondary_actions">
								<c:if test="${!param.disableSecondaryBrowseLink && !firstEntryBrowseSelected}">
									<a href="<c:out value="${secondaryBrowseUrl}" />" class="hier_entry_secondary_action" title="Browse structure starting from ${fn:toLowerCase(containerNode.resourceType)} <i>${containerNode.title}</i>">Structure</a>&nbsp;
								</c:if>
								<c:if test="${!param.disableSecondarySearchWithStateLink}">
									<c:choose>
										<c:when test="${retainedAsDirectMatch}">
											(<a href="<c:out value="${secondarySearchPathUrl}" />">View contents</a>)&nbsp;
										</c:when>
										<c:otherwise>
											(<a href="<c:out value="${secondarySearchWithStateUrl}" />">View contents</a>)&nbsp;
										</c:otherwise>
									</c:choose>
								</c:if>
								<c:if test="${!param.disableSecondarySearchPathLink}">
									(<a href="<c:out value="${secondarySearchPathUrl}" />">View all contents</a>)&nbsp;
								</c:if>
								<c:if test="${!param.disableSecondaryDetailsLink}">
									<a href="record?id=${containerNode.id}" class="hier_entry_secondary_action" title="View ${fn:toLowerCase(containerNode.resourceType)} information for ${containerNode.title}">Details</a>
								</c:if>
							</p>
						</c:if>
					</c:otherwise>
				</c:choose>
			</div>
		</c:set>
		
		<c:if test="${!fn:contains(excludeIDs, containerNode.id)}">
			<div class="hier_entry">
				${entryOut}
			</div>
			<c:if test="${showSeeAllLinks}">
				${viewAllResultsLink}
			</c:if>
		</c:if>
		<c:if test="${containerNode.resourceType != searchSettings.resourceTypeFile}">
			${"<div id='hier_container_children_"}${fn:replace(containerNode.id,":","-")}${"'>"}
		</c:if>
		${endContainerDivs}
	</c:if>
</c:forEach>