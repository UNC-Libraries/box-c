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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="settings" class="edu.unc.lib.dl.search.solr.util.SearchSettings" scope="page"/>
<c:set var="searchStateUrl" value="${requestScope.searchStateUrl}"/>

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${not empty param.containerResourceType}"><c:set var="containerResourceType" value="${param.containerResourceType}"/></c:when>
	<c:otherwise><c:set var="containerResourceType" value="${searchSettings.resourceTypeFolder}"/></c:otherwise>
</c:choose>

<div class="contentarea">
	<c:if test="${param.title != null}">
		<h2><c:out value="${param.title}" /></h2>
	</c:if>
	<form method="get" action="basicSearch">
		<input type="hidden" name="queryPath" value="${queryPath}"/>
		<div id="fpsearch_inputwrap">
			<input type="text" id="fpsearch_text" name="query">
			<select id="fpsearch_select" name="queryType">
				<option value="${searchSettings.searchFieldParams[searchFieldKeys.DEFAULT_INDEX]}"><c:out value="${searchSettings.searchFieldLabels[searchFieldKeys.DEFAULT_INDEX]}"/></option>
				<option value="${searchSettings.searchFieldParams[searchFieldKeys.TITLE_INDEX]}"><c:out value="${searchSettings.searchFieldLabels[searchFieldKeys.TITLE_INDEX]}"/></option>
				<option value="${searchSettings.searchFieldParams[searchFieldKeys.CONTRIBUTOR_INDEX]}"><c:out value="${searchSettings.searchFieldLabels[searchFieldKeys.CONTRIBUTOR_INDEX]}"/></option>
				<option value="${searchSettings.searchFieldParams[searchFieldKeys.SUBJECT_INDEX]}"><c:out value="${searchSettings.searchFieldLabels[searchFieldKeys.SUBJECT_INDEX]}"/></option>
			</select>
		</div>
		<div id="fpsearch_submitwrap">
			<input type="submit" value="Search" id="fpsearch_submit">
		</div>
	
		<c:if test="${param.showSearchWithin == 'true'}">
			<p class="fpsearch_search_within">
				<c:choose>
					<c:when test="${not empty searchState.facets[searchFieldKeys.ANCESTOR_PATH]}">
						<input type="radio" name="${searchSettings.searchStateParams['SEARCH_WITHIN']}" value="" /> Everything
						<c:url var="containerQuery" scope="page" value='${queryPath}'>
							<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${searchState.facets[searchFieldKeys.ANCESTOR_PATH].searchValue}"/>
						</c:url>
						<input type="radio" name="${searchSettings.searchStateParams['SEARCH_WITHIN']}" 
							value="${searchSettings.searchStateParams['FACET_FIELDS']}=<c:out value='${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${searchState.facets[searchFieldKeys.ANCESTOR_PATH].searchValue}' />" checked="checked" /> In current <c:out value="${containerResourceType}"/>
					</c:when>
					<c:otherwise>
						<input type="radio" name="${searchSettings.searchStateParams['SEARCH_WITHIN']}" value="" checked="checked" /> Everything
					</c:otherwise>
				</c:choose>
				<c:if test="${(not empty searchState.facets && ((not empty searchState.facets[searchFieldKeys.ANCESTOR_PATH] && fn:length(searchState.facets) > 1)
						|| empty searchState.facets[searchFieldKeys.ANCESTOR_PATH]))
						|| not empty searchState.rangeFields 
						|| not empty searchState.searchFields || not empty searchState.accessTypeFilter}">
					<c:set var="searchStateParameters" value='${fn:replace(searchStateUrl, "\\\"", "%22")}'/>
					<input type="radio" name="${searchSettings.searchStateParams['SEARCH_WITHIN']}" value="${searchStateParameters}"/> Within results
				</c:if>
			</p>
		</c:if>
		<p class="right fpsearch_advanced_link"><a href="advancedSearch">Advanced Search</a></p>
	</form>
	<c:if test="${param.showBrowse == 'true'}">
		<h2 class="fpsearch_browse_separator italics">or</h2>
		<div class="fpsearch_browse_links">
			<a href="search?types=Collection">Browse the Collections</a><br/>
			<a href="browseDepartments">Browse Departments</a>
		</div>
	</c:if>
</div>