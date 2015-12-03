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

<div class="gray">
	<div class="fourcol">
		<c:if test="${empty param.showBreadCrumbs || param.showBreadCrumbs}">
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
				<c:if test="${empty param.showFolderFacet || param.showFolderFacet}">
					<h2>Folders</h2>
					<div id="facet_field_path_structure" class="hidden">
						<c:if test="${not empty selectedContainer}"><c:set var="containerPath" value="/${selectedContainer.id}"/></c:if>
						<c:url var="structureUrl" scope="page" value='structure${containerPath}/path'>
							<c:param name="view" value="facet"/>
							<c:param name="queryp" value="list"/>
							<c:param name="files" value="false"/>
						</c:url>
						<a href="<c:out value="${structureUrl}" />"><img src="/static/images/ajax_loader.gif"/></a>
					</div>
				</c:if>
				<h2>Refine your search</h2>
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
	
	<div class="threecol">
		<c:choose>
			<c:when test="${empty param.showSearchBox || param.showSearchBox}">
				<div class="threecol lightest shadowtop searchwithin">
					<c:if test="${not empty selectedContainer}">
						<c:set var="containerResourceType" value="${fn:toLowerCase(resultResponse.selectedContainer.resourceType)}" scope="page"/>
					</c:if>
					<c:import url="common/searchBox.jsp">
						<c:param name="title">Search</c:param>
						<c:param name="showSearchWithin">true</c:param>
						<c:param name="containerResourceType">${containerResourceType}</c:param>
					</c:import>
				</div>
			</c:when>
			<c:otherwise>
				<c:set var="noSearchboxClass" value=" no_searchbox" />
			</c:otherwise>
		</c:choose>
		<div class="threecol white${noSearchboxClass}">
			<div class="contentarea">
				<c:choose>
					<c:when test="${resultCount > 0}">
						<c:import url="searchResults/resultsList.jsp" />
					</c:when>
					<c:otherwise>
						<c:import url="error/noResults.jsp"/>
					</c:otherwise>
				</c:choose>
			</div>
		</div>
	</div>
</div>