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

<div class="content-wrap">
<div class="contentarea">
	<h2>${pageSubtitle}</h2>
	<div class="results_header_hierarchy_path">
		<c:if test="${not empty resultResponse.selectedContainer}">
			<c:set var="facetNodes" scope="request" value="${resultResponse.selectedContainer.path.facetNodes}"/>
			<c:import url="common/hierarchyTrail.jsp">
				<c:param name="fieldKey">ANCESTOR_PATH</c:param>
				<c:param name="linkLast">true</c:param>
				<c:param name="displayHome">false</c:param>
				<c:param name="limitToContainer">true</c:param>
				<c:param name="ignoreSearchState">true</c:param>
			</c:import>
		</c:if>
	</div>
</div>
<c:set var="searchState" value="${resultResponse.searchState}"/>

<div class="gray">
	<div class="fourcol">
		<c:set var="searchState" scope="request" value="${searchState}"/>
		<c:if test="${resultType != 'collectionBrowse'}">
			<div class="fourcol light shadowtop breadcrumbs">
				<div class="contentarea">
					<c:if test="${not empty searchQueryUrl}">
					<h2>Breadcrumbs</h2>
					<c:import url="/jsp/util/breadCrumbs.jsp">
						<c:param name="searchStateParameters">${searchQueryUrl}</c:param>
					</c:import>
					</c:if>
				</div>
			</div>
		</c:if>
		<div class="fourcol gray">
			<div id="facetList" class="contentarea">
				<c:set var="facetFields" scope="request" value="${resultResponse.facetFields}"/>
				<c:choose>
					<c:when test="${resultType == 'collectionBrowse'}">
						
						<h2>Refine your results</h2>
						<c:import url="/jsp/util/facetList.jsp">
							<c:param name="queryMethod">search</c:param>
							<c:param name="searchStateParameters">${searchQueryUrl}</c:param>
						</c:import>
					</c:when>
					<c:otherwise>
						<c:set var="selectedContainer" scope="request" value="${resultResponse.selectedContainer}"/>
						<h2>Refine your search</h2>
						<c:import url="/jsp/util/facetList.jsp">
							<c:param name="queryMethod">search</c:param>
							<c:param name="searchStateParameters">${searchQueryUrl}</c:param>
						</c:import>
					</c:otherwise>
				</c:choose>
			</div>
		</div>
	</div>
	
	<div class="threecol">
		<c:if test="${resultType != 'collectionBrowse'}">
			<div class="threecol lightest shadowtop searchwithin">
				<c:if test="${not empty resultResponse.selectedContainer}">
					<c:set var="containerResourceType" value="${fn:toLowerCase(resultResponse.selectedContainer.resourceType)}" scope="page"/>
				</c:if>
				<c:import url="common/searchBox.jsp">
					<c:param name="title">Search</c:param>
					<c:param name="showSearchWithin">true</c:param>
					<c:param name="containerResourceType">${containerResourceType}</c:param>
				</c:import>
			</div>
		</c:if>
		<c:if test="${resultType == 'collectionBrowse'}">
			<c:set var="noSearchboxClass" value=" no_searchbox" />
		</c:if>
		<div class="threecol white${noSearchboxClass}">
			<div class="contentarea">
				<c:set var="resultCount" scope="request" value="${resultResponse.resultCount}"/>
				<c:choose>
					<c:when test="${resultCount > 0}">
						<div class="bottomline paddedline">
							<c:import var="navigationBar" url="searchResults/navigationBar.jsp">
								<c:param name="queryMethod">${queryMethod}</c:param>
							</c:import>
							${navigationBar}
							<c:import url="searchResults/sortForm.jsp">
								<c:param name="queryMethod">${queryMethod}</c:param>
								<c:param name="currentSort">${searchState.sortType}</c:param>
								<c:param name="currentSortOrder">${searchState.sortNormalOrder}</c:param>
							</c:import>
						</div>
						<c:if test="${not empty resultResponse.selectedContainer}">
							<c:set var="metadata" value="${resultResponse.selectedContainer}" scope="request"/>
							<c:import url="searchResults/selectedContainerEntry.jsp">
							</c:import>
						</c:if>
						<c:forEach items="${resultResponse.resultList}" var="metadataEntry" varStatus="status">
							<c:set var="metadata" scope="request" value="${metadataEntry}"/>
							<c:choose>
								<c:when test="${resultType == 'searchResults'}">
									<c:import url="searchResults/searchResultEntry.jsp">
										<c:param name="resultNumber" value="${status.count}"/>
									</c:import>
								</c:when>
								<c:when test="${resultType == 'collectionBrowse'}">
									<c:import url="searchResults/browseResultEntry.jsp">
										<c:param name="resultNumber" value="${status.count}"/>
									</c:import>
								</c:when>
							</c:choose>
						</c:forEach>
						<div class="topline">
							${navigationBar}
						</div>	
					</c:when>
					<c:otherwise>
						<c:import url="error/noResults.jsp"/>
					</c:otherwise>
				</c:choose>
			</div>
		</div>
	</div>
</div>
<script>
	var require = {
		config: {
			'searchResults' : {
				'filterParams' : '${cdr:urlEncode(searchQueryUrl)}'
			},
		}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/searchResults"></script>
</div>