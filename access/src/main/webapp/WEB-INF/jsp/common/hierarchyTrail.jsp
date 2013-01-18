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
<c:set var="fieldName" value="${searchSettings.searchFieldParams[param.fieldKey]}"/>

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${empty searchStateUrl || param.ignoreSearchState == true}">
		<c:set var="shiftFacetUrlBase" value="${queryPath}"/>
	</c:when>
	<c:otherwise>
		<c:set var="shiftFacetUrlBase" value="${queryPath}?${searchStateUrl}"/>
	</c:otherwise>
</c:choose>

<span class="hierarchicalTrail">  
	<c:if test="${param.displayHome == true }">
		<c:url var="shiftFacetUrl" scope="page" value='${shiftFacetUrlBase}'>
			<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["REMOVE_FACET"]}:${searchSettings.searchFieldParams["ANCESTOR_PATH"]}'/>
		</c:url>
		<a href="<c:out value="${shiftFacetUrl}"/>">Home</a>
	</c:if>
	<c:forEach items="${facetNodes}" var="facetNode" varStatus="status">
		<c:if test="${!status.first || param.displayHome}">
			&gt; 
		</c:if>
		<c:choose>
			<c:when test="${status.last && param.linkLast != true}">
				<c:out value="${facetNode.displayValue}" />
			</c:when>
			<c:otherwise>
				<c:url var="shiftFacetUrl" scope="page" value='${shiftFacetUrlBase}'>
					<c:choose>
						<c:when test="${param.limitToContainer == true}">
							<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${fieldName},"${facetNode.limitToValue}"'/>
						</c:when>
						<c:otherwise>
							<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["SET_FACET"]}:${fieldName},"${facetNode.searchValue}"'/>
						</c:otherwise>
					</c:choose>
					
				</c:url>
				<a href="<c:out value="${shiftFacetUrl}"/>"><c:out value="${facetNode.displayValue}" /></a>
			</c:otherwise>
		</c:choose>
	</c:forEach>
</span>