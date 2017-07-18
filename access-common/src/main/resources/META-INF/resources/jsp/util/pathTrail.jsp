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

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<c:choose>
	<c:when test="${empty searchStateUrl || param.ignoreSearchState == true}">
		<c:set var="shiftFacetUrlBase" value=""/>
	</c:when>
	<c:otherwise>
		<c:set var="shiftFacetUrlBase" value="?${searchStateUrl}"/>
	</c:otherwise>
</c:choose>

<span class="hierarchicalTrail">  
	<c:forEach items="${objectPath.entries}" var="pathEntry" varStatus="status">
		<c:if test="${!param.hideLast || !status.last}">
			<c:if test="${!status.first}">
				&gt; 
			</c:if>
			<c:choose>
				<c:when test="${status.last && param.linkLast != true}">
					<c:out value="${pathEntry.name}" />
				</c:when>
				<c:otherwise>
					<c:url var="shiftFacetUrl" scope="page" value="${queryPath}/${pathEntry.pid}${shiftFacetUrlBase}"></c:url>
					<a href="<c:out value="${shiftFacetUrl}"/>"><c:out value="${pathEntry.name}" /></a>
				</c:otherwise>
			</c:choose>
		</c:if>
	</c:forEach>
</span>