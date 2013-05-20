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
	<c:when test="${not empty param.resultOperation}"><c:set var="resultOperation" value="${param.resultOperation}"/></c:when>
	<c:otherwise><c:set var="resultOperation" value="list"/></c:otherwise>
</c:choose>
<span class="path_trail">  
	<c:if test="${param.displayHome == true }">
		<a href="<c:out value="${resultOperation}"/><c:if test="${not empty searchStateUrl && param.ignoreSearchState == false}">?${searchStateUrl}</c:if>">Home</a>
	</c:if>
	<c:forEach items="${facetNodes}" var="facetNode" varStatus="status">
		<c:if test="${!(status.last && param.skipLast)}">
			<c:if test="${!status.first || param.displayHome}">
				&gt; 
			</c:if>
			<c:choose>
				<c:when test="${status.last && param.linkLast != true}">
					<c:out value="${facetNode.displayValue}" />
				</c:when>
				<c:otherwise>
					<c:set var="shiftPathUrl">
						${resultOperation}/${facetNode.searchKey.replace(':', '/')}<c:if test="${not empty searchStateUrl && param.ignoreSearchState == false}">?${searchStateUrl}</c:if>
					</c:set>
					<a href="<c:out value="${shiftPathUrl}"/>"><c:out value="${facetNode.displayValue}" /></a>
				</c:otherwise>
			</c:choose>
		</c:if>
	</c:forEach>
</span>