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

<div class="columns">
	<div class="column is-one-quarter facets-border border-box-left-top">
	</div>
<%--<c:choose>
	<c:when test="${not empty facetFields}">
		<div class="column is-one-quarter facets-border border-box-left-top">
			<div class="facet-padding">
				<div id="facetList" class="contentarea">
					<h2 class="facet-header">Filter results by...</h2>
					<c:import url="/jsp/util/facetList.jsp">
						<c:param name="queryMethod"><c:choose>
							<c:when test="${not empty facetQueryMethod}">${facetQueryMethod}</c:when>
							<c:otherwise>${queryMethod}</c:otherwise>
						</c:choose></c:param>
						<c:param name="searchStateParameters">${searchQueryUrl}</c:param>
					</c:import>
				</div>
			</div>
		</div>
		<div class="column is-three-quarters search-results-border border-box-left-top">
	</c:when>
	<c:otherwise>
		<div class="column is-12 search-results-border border-box-left-top">
	</c:otherwise>
</c:choose>--%>
	<div class="column is-three-quarters search-results-border border-box-left-top">
	<c:import url="searchResults/resultsList.jsp" />
<%--		<c:choose>
			<c:when test="${resultCount > 0}">
				<c:import url="searchResults/resultsList.jsp" />
			</c:when>
			<c:otherwise>
				<div class="contentarea">
					<c:import url="error/noResults.jsp"/>
				</div>
			</c:otherwise>
		</c:choose>--%>
		</div>
</div>

<%--
<div class="columns is-mobile">
	<div class="column is-12 search-pagination-bottom">
		<c:import var="navigationBar" url="searchResults/navigationBar.jsp?showLinks=true">
			<c:param name="queryMethod">${queryMethod}</c:param>
			<c:param name="showPaginationLinks">true</c:param>
		</c:import>
		${navigationBar}
	</div>
</div>--%>
