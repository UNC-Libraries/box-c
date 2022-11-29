<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
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
<s:eval var="currentAbsoluteUrl" scope="request" expression="T(edu.unc.lib.boxc.common.util.URIUtil).join(accessBaseUrl, currentRelativeUrl)"/>

<c:url var="contactUrl" scope="request" value="https://library.unc.edu/wilson/contact/">
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