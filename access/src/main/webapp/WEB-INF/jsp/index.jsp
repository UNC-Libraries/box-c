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
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setTimeZone value="GMT" scope="session"/>
<c:choose>
	<c:when test="${empty requestScope['javax.servlet.forward.request_uri']}">
		<c:set var="currentUrl" value="${pageContext.request.requestURL}" />
	</c:when>
	<c:otherwise>
		<c:set var="currentUrl" value="${requestScope['javax.servlet.forward.request_uri']}" />
	</c:otherwise>
</c:choose>

<c:if test="${not empty pageContext.request.queryString}">
	<c:set var="currentUrl" value="${currentUrl}?${pageContext.request.queryString}"/>
</c:if>
<c:set var="currentRelativeUrl" scope="request" value="${currentUrl}"/>
<c:set var="currentAbsoluteUrl" scope="request" value="${pageContext.request.scheme}://${pageContext.request.serverName}${currentRelativeUrl}"/>

<c:url var="contactUrl" scope="request" value="http://blogs.lib.unc.edu/cdr/index.php/contact-us/">
	<c:param name="refer" value="${currentAbsoluteUrl}"/>
</c:url>

<c:choose>
	<c:when test="${template =='ajax'}">
		<c:import url="/jsp/template/ajaxTemplate.jsp" />
	</c:when>
	<c:otherwise>
		<c:import url="template/defaultTemplate.jsp" />
	</c:otherwise>
</c:choose>