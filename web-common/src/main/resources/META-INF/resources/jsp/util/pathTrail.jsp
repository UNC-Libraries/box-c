<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="record"/></c:otherwise>
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
		<c:choose>
			<c:when test="${status.last}">
				<span>&raquo;</span>
				<span><c:out value="${pathEntry.name}" /></span>
			</c:when>
			<c:when test="${status.index <= 2 || breadcrumbSize <= 5 || status.index == breadcrumbSize - 2}">
				<c:url var="shiftFacetUrl" scope="page" value="${queryPath}/${pathEntry.pid}${shiftFacetUrlBase}"></c:url>
				<c:choose>
					<c:when test="${status.index == 0}">
						<a href="<c:out value="/collections"/>">Collections</a>
					</c:when>
					<c:otherwise>
						<span>&raquo;</span>
						<a href="<c:out value="${shiftFacetUrl}"/>"><c:out value="${pathEntry.name}" /></a>
					</c:otherwise>
				</c:choose>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test="${status.index == 3}">
						<span>&raquo;</span>
						<a id="expand-breadcrumb" href="#">&hellip;</a>
					</c:when>
					<c:otherwise>
						<span class="full-crumb hidden">&raquo;</span>
					</c:otherwise>
				</c:choose>
				<c:url var="shiftFacetUrl" scope="page" value="${queryPath}/${pathEntry.pid}${shiftFacetUrlBase}"></c:url>
				<a href="<c:out value="${shiftFacetUrl}"/>" class="full-crumb hidden"><c:out value="${pathEntry.name}" /></a>


			</c:otherwise>
		</c:choose>
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