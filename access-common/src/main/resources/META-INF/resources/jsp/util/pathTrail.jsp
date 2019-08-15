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

<c:set var="breadcrumbSize" value="${objectPath.entries.size()}"/>
<span class="hierarchicalTrail">
	<c:forEach items="${objectPath.entries}" var="pathEntry" varStatus="status">
		<c:if test="${!param.hideLast || !status.last}">
			<c:if test="${status.index > 1}">
				&raquo;
			</c:if>
			<c:choose>
				<c:when test="${status.index <= 3}">
					<c:if test="${status.index > 0}">
						<c:url var="shiftFacetUrl" scope="page" value="${queryPath}/${pathEntry.pid}${shiftFacetUrlBase}"></c:url>
						<c:choose>
							<c:when test="${status.index == 1 && status.index == breadcrumbSize - 1}">
								<span>Collections</span>
							</c:when>
							<c:when test="${status.index == 1}">
								<a href="<c:out value="${shiftFacetUrl}"/>">Collections</a>
							</c:when>
							<c:when test="${status.index == breadcrumbSize - 1}">
								<span><c:out value="${pathEntry.name}" /></span>
							</c:when>
							<c:otherwise>
								<a href="<c:out value="${shiftFacetUrl}"/>"><c:out value="${pathEntry.name}" /></a>
							</c:otherwise>
						</c:choose>
					</c:if>
				</c:when>
				<c:otherwise>
					<c:if test="${status.index == 4}">
						<a id="expand-breadcrumb" href="#">&hellip;</a>
					</c:if>
					<c:choose>
						<c:when test="${status.index == breadcrumbSize - 2}">
							<span class="full-crumb hidden"><c:out value="${pathEntry.name}" /></span>
						</c:when>
						<c:otherwise>
							<c:url var="shiftFacetUrl" scope="page" value="${queryPath}/${pathEntry.pid}${shiftFacetUrlBase}"></c:url>
							<a href="<c:out value="${shiftFacetUrl}"/>" class="full-crumb hidden"><c:out value="${pathEntry.name}" /></a>
						</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
		</c:if>
	</c:forEach>
</span>
<script>
	(function() {
		var crumb_trail = document.getElementById('expand-breadcrumb');

		if (crumb_trail !== null) {
			crumb_trail.addEventListener('click', showFullCrumb);

			function showFullCrumb(e) {
				e.preventDefault();
				crumb_trail.classList.add('hidden');
				var full_crumb = document.querySelectorAll('.full-crumb');
				full_crumb.forEach(function(crumb) {
					crumb.classList.remove('hidden');
				});
			}
		}
	})();
</script>