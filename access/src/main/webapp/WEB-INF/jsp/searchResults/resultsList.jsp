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

<c:if test="${not empty selectedContainer && (empty param.showSelectedContainer || param.showSelectedContainer)}">
	<c:set var="metadata" value="${selectedContainer}" scope="request"/>
	<c:import url="searchResults/selectedContainerEntry.jsp">
	</c:import>
	
	<c:set var="collectionName"><c:out value='${metadata.parentCollectionName}' /></c:set>
	<c:if test="${empty collectionName && metadata.resourceType == 'Collection'}">
		<c:set var="collectionName"><c:out value='${metadata.title}' /></c:set>
	</c:if>
	<c:if test="${empty collectionName}">
		<c:set var="collectionName" value="(no collection)" />
	</c:if>
	<c:set var="gaCommands" scope="request">${gaCommands} ga('unc.send', 'event', '${collectionName}', 'list', '<c:out value="${metadata.title}|${metadata.pid}" />');</c:set>
</c:if>
<c:forEach items="${resultResponse.resultList}" var="metadataEntry" varStatus="status">
	<c:set var="metadata" scope="request" value="${metadataEntry}"/>
	<c:choose>
		<c:when test="${empty param.entryTemplate}">
			<c:import url="searchResults/searchResultEntry.jsp"/>
		</c:when>
		<c:otherwise>
			<c:import url="${param.entryTemplate}" />
		</c:otherwise>
	</c:choose>
</c:forEach>
<div class="topline">
	${navigationBar}
</div>