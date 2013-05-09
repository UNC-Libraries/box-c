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
	queryPath - override the servlet that facet links will send the user to.  Default "search"
	displayCounts - Boolean, indicates if child counts should be displayed next to contain names.  From param scope.
	ignoreSearchState - If true, then preexisting search state parameters will not be included in container urls. 
	applyCutoffs - Determines if hierarchical facet cutoffs are appended added facets when selecting containers
	displaySecondaryActions - whether or not ot show the secondary actions for each container
--%>
<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.applyCutoffs}"><c:set var="applyCutoffs" value="${param.applyCutoffs}"/></c:when>
	<c:otherwise><c:set var="applyCutoffs" value="true"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.filePrimaryDownload}"><c:set var="filePrimaryDownload" value="${param.filePrimaryDownload}"/></c:when>
	<c:otherwise><c:set var="filePrimaryDownload" value="false"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.showSeeAllLinks}"><c:set var="showSeeAllLinks" value="${param.showSeeAllLinks}"/></c:when>
	<c:otherwise><c:set var="showSeeAllLinks" value="true"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.root}"><c:set var="hideRoot" value="${!param.root}"/></c:when>
	<c:otherwise><c:set var="hideRoot" value="false"/></c:otherwise>
</c:choose>

<c:set var="displaySecondaryActions" value="${param.secondary == 'true'}"/>

<c:choose>
	<c:when test="${not empty param.excludeIDs}"><c:set var="excludeIDs" value="${param.excludeIDs}"/></c:when>
	<c:otherwise><c:set var="excludeIDs" value=""/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${empty searchStateUrl || param.ignoreSearchState == true}">
		<c:set var="containerUrlBase" value=""/>
	</c:when>
	<c:otherwise>
		<c:set var="containerUrlBase" value="?${sessionScope.recordNavigationState.searchStateUrl}"/>
	</c:otherwise>
</c:choose>

<cdr:hierarchicalTree items="${structureResults}" var="currentNode" hideRoot="${hideRoot}" excludeIds="${excludeIds}">
	<c:set var="containerNode" value="${currentNode.metadata}"/>
	
	<c:set var="isAContainer" value="${containerNode.resourceType != searchSettings.resourceTypeFile}" />

	<c:choose>
		<c:when test="${not empty containerNode.countMap}">
			<c:set var="childCount" value="${containerNode.countMap.child}"/>
		</c:when>
		<c:otherwise>
			<c:set var="childCount" value="0"/>
		</c:otherwise>
	</c:choose>
	
	<c:set var="containerFacetAction">
		<c:choose>
			<c:when test="${!isAContainer}">
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
	
	<c:set var="retainedAsDirectMatch" value="${fn:length(hierarchicalViewResults.matchingContainerPids) > 0 && childCount == 0}" />
	
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
		<c:when test="${queryPath == 'structure'}">
			<c:url var="primaryUrl" scope="page" value='structure/${containerBean.pid.path}'>
			</c:url>
		</c:when>
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
	
	<%-- Render the expand/collapse icon --%>
	<c:if test="${isAContainer}">
		<%-- Determine whether to display a collapse or expand icon --%>
		<c:choose>
			<c:when test="${(empty childCount || childCount == 0) && retainedAsDirectMatch}">
				<div class="cont_toggle" title="Matched your query"></div>
			</c:when>
			<c:when test="${fn:length(currentNode.children) > 0 && (isRootNode ||
					hierarchicalViewResults.searchState.rowsPerPage == 0)}">
				<div class="cont_toggle collapse" title="Collapse contents"></div>
			</c:when>
			<c:when test="${childCount == 0}">
				<div class="cont_toggle" title="No contents returned"></div>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test="${fn:length(currentNode.children) > 0}">
						<%-- Subcontainer children present means that expanding should just get non-container children --%>
						<c:url var="expandUrl" value="structure/${containerNode.pid.path}?${searchParams}">
							<c:param name="tier" value="${containerNode.path.searchValue}"/>
							<c:param name="files" value="only"/>
							<c:param name="disableSecondaryDetailsLink" value='${param.disableSecondaryDetailsLink}'/>
						</c:url>
					</c:when>
					<c:otherwise>
						<c:url var="expandUrl" value="structure/${containerNode.pid.path}?${searchParams}">
							<c:param name="depth" value='1'/>
							<c:param name="view" value='ajax'/>
							<c:param name="root" value='false'/>
						</c:url>
					</c:otherwise>
				</c:choose>
				<div class="cont_toggle expand" title="Expand contents" data-url="<c:out value='${expandUrl}' />"></div>
			</c:otherwise>
		</c:choose>
	</c:if>
	
	<%-- Display the resource type icon --%>
	<div class="resource_icon ${containerNode.resourceType}" title="${containerNode.resourceType}"></div>
	
	<%-- Display the main entry description --%>
	<div class="description">
		<c:choose>
			<c:when test="${firstEntryBrowseSelected}">
				<a class="primary_action" title="Currently browsing ${containerNode.title}">${containerNode.title}</a>
			</c:when>
			<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile || containerNode.resourceType == searchSettings.resourceTypeAggregate}">
				<c:choose>
					<c:when test="${filePrimaryDownload}">
						<c:url var="filePrimaryUrl" scope="page" value="${cdr:getDatastreamUrl(containerNode, 'DATA_FILE', fedoraUtil)}&dl=true"/>
						<a href="<c:out value='${filePrimaryUrl}' />" class="primary_action" title="Download this item.">${containerNode.title}</a>
					</c:when>
					<c:otherwise>
						<c:url var="filePrimaryUrl" value="record">
							<c:param name="id" value="${containerNode.id}"/>
						</c:url>
						<a href="<c:out value='${filePrimaryUrl}' />" class="primary_action" title="View details for this item.">${containerNode.title}</a>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:otherwise>
				<a href="<c:out value='${primaryUrl}' />" class="primary_action" title="${primaryTooltip}">${containerNode.title}</a>
			</c:otherwise>
		</c:choose>
		
		<c:choose>
			<c:when test="${!isAContainer}">
				<c:if test="${displaySecondaryActions}">
					<p class="secondary_actions">
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
					<span class="count">(${childCount})</span>
				</c:if>
				
				<c:if test="${displaySecondaryActions}">
					<p class="secondary_actions">
						<a href="record?id=${containerNode.id}" class="secondary_action" title="View ${fn:toLowerCase(containerNode.resourceType)} information for ${containerNode.title}">Details</a>
					</p>
				</c:if>
			</c:otherwise>
		</c:choose>
	</div>
	<%-- Render the "see all" link after the last non-container result --%>
	<c:if test="${lastSibling && !isAContainer}">
		${"</div></div>"}${"<div class='entry_wrap'><div class='entry'>"}<a href="${containerUrl}">(see all)</a>
	</c:if>
</cdr:hierarchicalTree>