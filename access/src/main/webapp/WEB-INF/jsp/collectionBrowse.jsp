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

<div>
	<div class="search-query-text collection-list has-text-centered">
		<h2>${pageSubtitle}</h2>
	</div>
	<c:set var="searchState" value="${resultResponse.searchState}"/>
	<c:set var="facetFields" scope="request" value="${resultResponse.facetFields}"/>
	<c:set var="selectedContainer" scope="request" value="${resultResponse.selectedContainer}"/>
	<c:set var="resultCount" scope="request" value="${resultResponse.resultCount}"/>

	<div class="columns is-mobile">
		<div class="column is-12 collection-browse">
			<c:choose>
				<c:when test="${resultCount > 0}">
					<c:import url="searchResults/resultsList.jsp">
						<c:param name="entryTemplate">searchResults/browseResultEntry.jsp</c:param>
						<c:param name="excludeNavigationBar">true</c:param>
					</c:import>
				</c:when>
				<c:otherwise>
					<c:import url="error/noResults.jsp" />
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/collectionBrowse"></script>