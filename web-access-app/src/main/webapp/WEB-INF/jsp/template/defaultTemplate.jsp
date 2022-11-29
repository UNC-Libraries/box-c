<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="en">
<head>
	<c:set var="url">${accessBaseUrl}</c:set>
	<base href="${url}" />
	<%@ include file="../../html/headElements.html"%>
	<title>
		Digital Collections Repository<c:if test="${not empty pageSubtitle}"> - <c:out value="${pageSubtitle}"/></c:if>
	</title>
</head>
<body>
<c:set var="gaCommands" value="" scope="request" />
<div id="pagewrap">
	<div id="pagewrap_inside">
		<c:choose>
			<c:when test="${contentPage.equals('frontPage.jsp')}">
				<c:import url="common/header.jsp" />
			</c:when>
			<c:otherwise>
				<c:import url="common/headerSmall.jsp" />
			</c:otherwise>
		</c:choose>
		<div id="content">
			<c:choose>
				<c:when test="${not empty contentPage}">
					<c:import url="${contentPage}" />
				</c:when>
				<c:otherwise>
					<c:import url="error/404.jsp" />
				</c:otherwise>
			</c:choose>
		</div>
		<c:import url="common/footer.jsp"/>
	</div>
</div>
<c:import url="googleAnalytics.jsp" />
</body>
</html>
