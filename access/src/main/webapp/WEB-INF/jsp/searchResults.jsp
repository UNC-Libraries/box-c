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
			<c:set var="objectPath" scope="request" value="${resultResponse.selectedContainer.objectPath}"/>
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="linkLast">true</c:param>
				<c:param name="queryPath">list</c:param>
				<c:param name="ignoreSearchState">true</c:param>
			</c:import>
		</c:if>
	</div>
</div>
<c:set var="searchState" scope="request" value="${resultResponse.searchState}"/>
<c:set var="facetFields" scope="request" value="${resultResponse.facetFields}"/>
<c:set var="selectedContainer" scope="request" value="${resultResponse.selectedContainer}"/>
<c:set var="resultCount" scope="request" value="${resultResponse.resultCount}"/>
<c:import url="searchResults/resultsPage.jsp"/>
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