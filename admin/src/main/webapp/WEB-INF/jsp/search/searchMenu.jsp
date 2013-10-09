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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<div id="search_menu">
	<div class="query_menu">
		<div>
			<h3>Search</h3>
			<div>
				<form id="search_menu_form" method="get" action="doSearch${containerPath}">
					<div class="search_inputwrap">
						<input name="query" class="search_text" type="text" placeholder="Search"/>
						<select name="queryType" class="index_select">
							<option value="anywhere">Anywhere</option>
							<option value="titleIndex">Title</option>
							<option value="contributorIndex">Contributor</option>
							<option value="subjectIndex">Subject</option>
						</select>
					</div>
					<c:if test="${not empty resultResponse.selectedContainer}">
						<input type="hidden" name="container" value="${resultResponse.selectedContainer.id}"/>
					</c:if>
					<c:set var="searchStateParameters" value='${fn:replace(searchQueryUrl, "\\\"", "%22")}'/>
					<input type="hidden" name="within" value="${searchStateParameters}"/>
					<input type="submit" value="Go" class="search_submit"/>
					<p class="search_within">
						<input type="radio" value="" name="searchType" value=""/>Everything
						<c:if test="${not empty resultResponse.selectedContainer}">
							<input type="radio" checked="checked" name="searchType" value="container"/>Current folder
						</c:if>
						<input type="radio" name="searchType" value="within"/>Within results
					</p>
				</form>
				<c:import url="/jsp/util/breadCrumbs.jsp">
					<c:param name="searchStateParameters">${searchQueryUrl}</c:param>
				</c:import>
			</div>
		</div>
	</div>
	<div class="filter_menu">
		<div>
			<h3>Structure</h3>
			<c:set var="structureDataUrl">structure/<c:if test="${not empty resultResponse.selectedContainer}">${resultResponse.selectedContainer.id}/</c:if>collection<c:if test="${not empty searchStateParameters}">?${searchStateParameters}</c:if></c:set>
			<div data-href="${structureDataUrl}" id="structure_facet">
				<div class="center"><img src="/static/images/admin/loading_small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Descriptive</h3>
			<c:set var="facetsDataUrl">facets<c:if test="${not empty resultResponse.selectedContainer}">/${resultResponse.selectedContainer.id}</c:if>?facetSelect=collection,dept,language,subject<c:if test="${not empty searchStateParameters}">&${searchStateParameters}</c:if></c:set>
			<div data-href="${facetsDataUrl}">
				<div class="center"><img src="/static/images/admin/loading_small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Access Control</h3>
			<c:set var="facetsDataUrl">facets<c:if test="${not empty resultResponse.selectedContainer}">/${resultResponse.selectedContainer.id}</c:if>?facetSelect=role,status<c:if test="${not empty searchStateParameters}">&${searchStateParameters}</c:if></c:set>
			<div data-href="${facetsDataUrl}">
				<div class="center"><img src="/static/images/admin/loading_small.gif"/></div>
			</div>
		</div>
		<div>
			<h3>Content</h3>
			<c:set var="facetsDataUrl">facets<c:if test="${not empty resultResponse.selectedContainer}">/${resultResponse.selectedContainer.id}</c:if>?facetSelect=format,type,model,contentStatus<c:if test="${not empty searchStateParameters}">&${searchStateParameters}</c:if></c:set>
			<div data-href="${facetsDataUrl}">
				<div class="center"><img src="/static/images/admin/loading_small.gif"/></div>
			</div>
		</div>
	</div>
</div>