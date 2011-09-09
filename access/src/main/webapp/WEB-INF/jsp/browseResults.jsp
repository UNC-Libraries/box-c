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

<script type="text/javascript" src="/static/js/browseResults.js"></script>

<div class="contentarea">
	<h2>Structure browse</h2>
	<div class="results_header_hierarchy_path">
		<c:if test="${not empty selectedContainer}">
			<c:set var="facetTiers" scope="request" value="${selectedContainer.path.facetTiers}"/>
			<c:import url="WEB-INF/jsp/common/hierarchyTrail.jsp">
				<c:param name="fieldKey"><c:out value="${searchFieldKeys.ANCESTOR_PATH}"/></c:param>
				<c:param name="linkLast">true</c:param>
				<c:param name="displayHome">true</c:param>
				<c:param name="limitToContainer">false</c:param>
				<c:param name="ignoreSearchState">true</c:param>
				<c:param name="queryPath">browse</c:param>
			</c:import>
		</c:if>
	</div>
</div>
<c:set var="searchState" value="${resultResponse.searchState}"/>

<div class="gray">
	<div class="fourcol">
		<div class="fourcol light shadowtop breadcrumbs">
			<c:set var="searchState" scope="request" value="${searchState}"/>
			<c:import url="WEB-INF/jsp/searchResults/breadCrumbs.jsp">
				<c:param name="queryPath" value="browse"/>
			</c:import>
		</div>
		<div class="fourcol gray">
			<c:set var="facetFields" scope="request" value="${resultResponse.facetFields}"/>
			<c:import url="WEB-INF/jsp/common/facetList.jsp">
				<c:param name="queryPath" value="browse"/>
				<c:param name="title" value="Refine your results"/>
			</c:import>
		</div>
	</div>
	
	<div class="threecol">
		<div class="threecol lightest shadowtop searchwithin">
			<c:if test="${not empty searchState.facets[searchFieldKeys.ANCESTOR_PATH] && resultResponse.resultCount > 0}">
				<c:set var="containerResourceType" scope="page">${fn:toLowerCase(resultResponse.resultList[0].resourceType)}</c:set>
			</c:if>
			<c:import url="WEB-INF/jsp/common/searchBox.jsp">
				<c:param name="title">Search in structure view</c:param>
				<c:param name="showSearchWithin">true</c:param>
				<c:param name="queryPath" value="browse"/>
				<c:param name="containerResourceType">${containerResourceType}</c:param>
			</c:import>
		</div>
		<div id="hierarchical_view_browse" class="threecol white">
			<div class="contentarea">
				<c:set var="hierarchicalViewResults" scope="request" value="${resultResponse}"/>
				<c:import url="WEB-INF/jsp/browseResults/hierarchicalBrowse.jsp">
					<c:param name="queryPath" value="search"/>
					<c:param name="applyCutoffs" value="true"/>
					<c:param name="displayCounts" value="true"/>
					<c:param name="displaySecondaryActions" value="true"/>
					<c:param name="disableSecondarySearchPathLink" value="true"/>
					<c:param name="disableSecondaryBrowseLink" value="true"/>
					<c:param name="disableSecondarySearchWithStateLink" value="true"/>
					<c:param name="hideTypeIcon">false</c:param>
				</c:import>
			</div>						
		</div>
	</div>
</div>