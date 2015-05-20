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
				<option value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}"><c:out value="${searchSettings.searchFieldLabels['DEFAULT_INDEX']}"/></option>
				<option value="${searchSettings.searchFieldParams['TITLE_INDEX']}"><c:out value="${searchSettings.searchFieldLabels['TITLE_INDEX']}"/></option>
				<option value="${searchSettings.searchFieldParams['CONTRIBUTOR_INDEX']}"><c:out value="${searchSettings.searchFieldLabels['CONTRIBUTOR_INDEX']}"/></option>
				<option value="${searchSettings.searchFieldParams['SUBJECT_INDEX']}"><c:out value="${searchSettings.searchFieldLabels['SUBJECT_INDEX']}"/></option>
			</select>
		</div>
		<div id="fpsearch_submitwrap">
			<input type="submit" value="Search" id="fpsearch_submit">
		</div>
	
		<c:if test="${param.showSearchWithin == 'true'}">
			<c:set var="searchStateParameters" value='${fn:replace(searchQueryUrl, "\\\"", "%22")}'/>
			<c:if test="${not empty resultResponse.selectedContainer}">
				<input type="hidden" name="container" value="${resultResponse.selectedContainer.id}"/>
			</c:if>
			<input type="hidden" name="within" value="${searchStateParameters}"/>
			<p class="fpsearch_search_within">
			
				<input type="radio" name="searchType" value="" />Everything
				<c:if test="${not empty resultResponse.selectedContainer}">
					<input type="radio" checked="checked" name="searchType" value="container"/>In <c:out value="${containerResourceType}"/>
				</c:if>
				<c:if test="${(not empty searchState.facets && ((not empty searchState.facets['ANCESTOR_PATH'] && fn:length(searchState.facets) > 1)
						|| empty searchState.facets['ANCESTOR_PATH']))
						|| not empty searchState.rangeFields || not empty searchState.searchFields}">
					<input type="radio" name="searchType" value="within"/> Within results
				</c:if>
			</p>
		</c:if>
		<p class="right fpsearch_advanced_link"><a href="advancedSearch">Advanced Search</a></p>
	</form>
	<c:if test="${param.showBrowse == 'true'}">
		<h2 class="fpsearch_browse_separator italics">or</h2>
		<div class="fpsearch_browse_links">
			<a href="collections">Browse the Collections</a><br/>
			<a href="browse/depts/">Browse Departments</a>
		</div>
	</c:if>
</div>