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
	<h2>Structure browse</h2>
	<div class="results_header_hierarchy_path">
		<c:if test="${not empty selectedContainer}">
			<c:set var="objectPath" scope="request" value="${resultResponse.selectedContainer.objectPath}"/>
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="linkLast">true</c:param>
				<c:param name="ignoreSearchState">true</c:param>
				<c:param name="queryPath">structure</c:param>
			</c:import>
		</c:if>
	</div>
</div>
<c:set var="searchState" value="${resultResponse.searchState}"/>

<div>
	<div class="fourcol">
		<div class="fourcol light shadowtop breadcrumbs">
			<div class="contentarea">
				<h2>Breadcrumbs</h2>
				<c:set var="searchState" scope="request" value="${searchState}"/>
				<c:import url="/jsp/util/breadCrumbs.jsp">
					<c:param name="queryMethod" value="structure"/>
				</c:import>
			</div>
		</div>
		<div class="fourcol gray">
			<div id="facetList" class="contentarea">
				<h2>Refine your results</h2>
				<c:set var="facetFields" scope="request" value="${resultResponse.facetFields}"/>
				<c:import url="/jsp/util/facetList.jsp">
					<c:param name="queryMethod" value="structure"/>
					<c:param name="title" value="Refine your results"/>
				</c:import>
			</div>
		</div>
	</div>
	
	<div class="threecol">
		<div class="threecol lightest shadowtop searchwithin">
			<c:if test="${not empty searchState.facets['ANCESTOR_PATH'] && resultResponse.resultCount > 0}">
				<c:set var="containerResourceType" scope="page">${fn:toLowerCase(resultResponse.resultList[0].resourceType)}</c:set>
			</c:if>
			<c:import url="common/searchBox.jsp">
				<c:param name="title">Search in structure view</c:param>
				<c:param name="showSearchWithin">true</c:param>
				<c:param name="queryMethod" value="structure"/>
				<c:param name="containerResourceType">${containerResourceType}</c:param>
			</c:import>
		</div>
		<div class="threecol white">
			<div class="structure search contentarea">
			</div>
		</div>
	</div>
</div>
<script>
	var require = {
		config: {
			'structureBrowse' : {
				'results': ${resultJSON},
				'filterParams' : '${cdr:urlEncode(searchParams)}'
			},
		}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/structureBrowse"></script>
</div>
