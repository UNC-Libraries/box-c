<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="en">
<head>
	<c:set var="url">${adminBaseUrl}</c:set>
    <base href="${url}" />
	<c:import url="common/headElements.jsp" />
	<title>
		DCR Administration <c:if test="${not empty pageSubtitle}"> - <c:out value="${pageSubtitle}"/></c:if>
	</title>
</head>
<body>

<div id="pagewrap">
	<div id="pagewrap_inside">
		<c:import url="common/header.jsp" />
		<div id="content">
			<c:choose>
				<c:when test="${not empty contentPage}">
					<c:import url="${contentPage}" />
					<div id="vue-admin-app"></div>
					<script type="module" crossorigin src="/static/js/vue-admin-index.js"></script>
				</c:when>
				<c:otherwise>
					<c:import url="error/404.jsp" />
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>
</body>
</html>