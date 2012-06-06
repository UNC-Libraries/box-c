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

<c:if test="${not empty param.countLinkPath}">
	<c:set var="countLinkUrlBase" value="${param.countLinkPath}?${sessionScope.recordNavigationState.searchStateUrl}"/>
</c:if>

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
<c:forEach items="${hierarchicalViewResults.resultList}" var="containerNode" varStatus="resultStatus">
	<c:if test="${!(param.excludeParent && resultStatus.first)}">
		<c:set var="endContainerDivs" value=""/>
		<c:set var="indentCode" value=""/> 
		<div class="hier_entry">
			<c:set var="retainedAsDirectMatch" value="${fn:length(hierarchicalViewResults.matchingContainerPids) > 0
						&& (containerNode.childCount == 0
						|| hierarchicalViewResults.subcontainerCounts[containerNode.path.facetTiers[fn:length(containerNode.path.facetTiers) - 1].identifier] == containerNode.childCount)}"/>
			
			<c:set var="containerFacetAction">
				<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.ancestorPath.searchValue},${containerNode.ancestorPath.highestTier + 1}"
					</c:when>
					<c:when test="${applyCutoffs}">
						${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"
					</c:when>
					<c:otherwise>
						${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue}"
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
					<c:set var="primaryTooltip">View ${containerNode.childCount} matching item(s) contained within ${fn:toLowerCase(containerNode.resourceType)}&nbsp;${containerNode.title}</c:set>
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
			<c:set var="hasSubcontainers" value="${hierarchicalViewResults.subcontainerCounts[containerNode.path.facetTiers[fn:length(containerNode.path.facetTiers) - 1].identifier] > 0}" />
			<c:if test="${!resultStatus.first || empty hierarchicalViewResults.searchState.facets[searchFieldKeys.ANCESTOR_PATH]}">
				<c:set var="precedingTierCount" value="${hierarchicalViewResults.subcontainerCounts[rootNode.path.facetTiers[fn:length(rootNode.path.facetTiers) - 1].identifier] - 1}"/>
				<c:forEach var="pathTier" items="${containerNode.path.facetTiers}" varStatus="status">
					<c:if test="${pathTier.tier >= baseTier}">
						<c:set var="subcontainerCount" value="${hierarchicalViewResults.subcontainerCounts[pathTier.identifier]}"/>
						<c:choose>
							<c:when test="${status.last && (precedingTierCount == subcontainerCount 
									|| (empty subcontainerCount && precedingTierCount == 0))}">
								<c:set var="indentCode" value="${indentCode}0"/>
								
								${leadupIndent}<div class="indent_unit hier_container"></div><%-- 
								No spaces here otherwise it will show up in display
							--%></c:when>
							<c:when test="${status.last && precedingTierCount > 0 && 
									(empty subcontainerCount || precedingTierCount > subcontainerCount)}">
								<c:set var="indentCode" value="${indentCode}1"/>
								${leadupIndent}<div class="indent_unit hier_container hier_with_siblings"></div><%-- 
								No spaces here otherwise it will show up in display
							--%></c:when>
							<c:when test="${precedingTierCount >= hierarchicalViewResults.subcontainerCounts[pathTier.identifier]}">
								<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit hier_with_siblings"></div>'/>
								<c:set var="indentCode" value="${indentCode}1"/>
								${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathTier.identifier) }
							</c:when>
							<c:when test="${pathTier.tier == baseTier && param.excludeParent}">
								${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathTier.identifier) }
							</c:when>
							<c:when test="${pathTier.tier == baseTier}">
								${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathTier.identifier) }
							</c:when>
							<c:otherwise>
								<c:set var="leadupIndent" value='${leadupIndent}<div class="indent_unit"></div>'/>
								${cdr:decrementLongMap(hierarchicalViewResults.subcontainerCounts, pathTier.identifier) }
								<c:set var="indentCode" value="${indentCode}0"/>
							</c:otherwise>
						</c:choose>
						<c:if test="${empty hierarchicalViewResults.subcontainerCounts[pathTier.identifier] 
								|| hierarchicalViewResults.subcontainerCounts[pathTier.identifier] == 0}">
							<c:if test="${containerNode.resourceType == searchSettings.resourceTypeFile
									&& status.count == containerNode.ancestorPath.highestTier}">
								<c:set var="viewAllResultsLink">
									<div class="hier_entry">
										${leadupIndent}
										<a href="<c:out value='${containerUrl}' />">(see all)</a>
									</div>
								</c:set>
							</c:if>
							<c:if test="${!(pathTier.tier == baseTier && param.excludeParent) 
											&& containerNode.resourceType != searchSettings.resourceTypeFile}">
								<c:set var="endContainerDivs" value="${endContainerDivs}</div>"/>
							</c:if>
							
						</c:if>
						<%-- Shows sub container counts by depth --%>
						<%-- <div style="float: right;">&nbsp;&nbsp;(${precedingTierCount}|${subcontainerCount})</div> --%>
						<c:set var="precedingTierCount" value="${hierarchicalViewResults.subcontainerCounts[pathTier.identifier]}"/>
					</c:if>
				</c:forEach>
			</c:if>
			
			<c:set var="firstEntryBrowseSelected" value="${queryPath == 'browse' && (containerNode.childCount == 0 ||
					(resultStatus.first && not empty hierarchicalViewResults.searchState.facets[searchFieldKeys.ANCESTOR_PATH]))}"/>
			
			<c:if test="${containerNode.resourceType != searchSettings.resourceTypeFile}">
				<c:set var="subcontainerCount" value="${hierarchicalViewResults.subcontainerCounts[containerNode.path.facetTiers[fn:length(containerNode.path.facetTiers) - 1].identifier]}" scope="page"/>
				<%-- Determine whether to display a collapse or expand icon --%>
				<c:choose>
					<c:when test="${(empty subcontainerCount || subcontainerCount == 0) && retainedAsDirectMatch}">
						<img src="/static/images/no_action.png"/>
					</c:when>
					<c:when test="${subcontainerCount > 0 &&
							((resultStatus.first && queryPath != 'browse') ||
							hierarchicalViewResults.searchState.rowsPerPage == 0)}">
						<a href="#" class="hier_container_collapse" title="Collapse ${containerNode.title}" id="container_toggle_${fn:replace(containerNode.id, ':', '-')}"><img src="/static/images/collapse.png"/></a>
					</c:when>
					<c:otherwise>
						<c:set var="actions" scope="page" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue}"'/>
						<c:if test="${hasSubcontainers}">
							<c:set var="actions" value='${actions}|${searchSettings.actions["SET_RESOURCE_TYPE"]}:${searchSettings.resourceTypeFile}'/>
						</c:if>
						<c:url var="expandUrl" value="#browse?${searchStateUrl}&depth=1&indentCode=${indentCode}&ajax=true">
							<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${actions}'/>
							<c:param name="disableSecondaryDetailsLink" value='${param.disableSecondaryDetailsLink}'/>
							<c:param name="hideTypeIcon" value='${param.hideTypeIcon}'/>
						</c:url>
						<a href="<c:out value='${expandUrl}' />" title="Expand ${containerNode.title}" class="hier_container_expand hier_container_not_loaded" id="container_toggle_${fn:replace(containerNode.id, ':', '-')}"><img src="/static/images/expand.png"/></a>
					</c:otherwise>
				</c:choose>
			</c:if>
			
			<c:if test="${param.hideTypeIcon == false }">
				<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeCollection}">
						<img src="/static/images/hier_collection.png" alt="Collection" title="Collection" class="resource_type"/>
					</c:when>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						<img src="/static/images/hier_file.png" alt="Collection" title="Collection" class="resource_type"/>
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
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						<c:url var="fullRecordUrl" value="record">
							<c:param name="id" value="${containerNode.id}"/>
						</c:url>
						<a href="<c:out value='${fullRecordUrl}' />" class="hier_entry_primary_action" title="View details for this item.">${containerNode.title}</a>
					</c:when>
					<c:otherwise>
						<a href="<c:out value='${primaryUrl}' />" class="hier_entry_primary_action" title="${primaryTooltip}">${containerNode.title}</a>
					</c:otherwise>
				</c:choose>
				
				<c:choose>
					<c:when test="${containerNode.resourceType == searchSettings.resourceTypeFile}">
						<c:if test="${param.displaySecondaryActions}">
							<p class="hier_secondary_actions">
								<c:if test="${cdr:contains(containerNode.datastream, 'DATA_FILE')}">
									<a href="${cdr:getDatastreamUrl(containerNode, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
									(<c:out value="${containerNode.contentType.highestTierDisplayValue}"/> 
									<c:if test="${not empty containerNode.filesize}">
										&nbsp;<c:out value="${cdr:formatFilesize(containerNode.filesize, 1)}"/>
									</c:if>)
								</c:if>
							</p>
						</c:if>
					</c:when>
					<c:otherwise>
						<c:if test="${param.displayCounts && !(retainedAsDirectMatch && containerNode.childCount == 0)}">
							<span class="hier_count">(${containerNode.childCount})</span>
						</c:if>
						
						<c:if test="${param.displaySecondaryActions}">
							<c:url var="secondaryBrowseUrl" scope="page" value='browse${containerUrlBase}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue}"'/>
							</c:url>
							
							<c:url var="secondarySearchWithStateUrl" scope="page" value='search?${sessionScope.recordNavigationState.searchStateUrl}'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
							</c:url>
							
							<c:url var="secondarySearchPathUrl" scope="page" value='search'>
								<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]},"${containerNode.path.searchValue},${containerNode.path.highestTier + 1}"'/>
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
		</div>
		${viewAllResultsLink}
		<c:if test="${containerNode.resourceType != searchSettings.resourceTypeFile}">
			${"<div id='hier_container_children_"}${fn:replace(containerNode.id,":","-")}${"'>"}
		</c:if>
		${endContainerDivs}
	</c:if>
</c:forEach>