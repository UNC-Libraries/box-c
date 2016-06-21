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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html>
<head>
	<c:set var="url">${pageContext.request.requestURL}</c:set>
	<base href="${fn:substring(url, 0, fn:length(url) - fn:length(pageContext.request.requestURI))}${pageContext.request.contextPath}/" />
	<%@ include file="../../html/headElements.html"%>
	<title>
		Carolina Digital Repository<c:if test="${not empty pageSubtitle}"> - <c:out value="${pageSubtitle}"/></c:if>
	</title>
</head>
<body>
<c:set var="gaCommands" value="" scope="request" />
<div id="pagewrap">
	<div id="pagewrap_inside">
		<c:import url="common/header.jsp" />
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
