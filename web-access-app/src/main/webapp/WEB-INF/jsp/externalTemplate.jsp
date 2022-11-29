<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<div class="content-wrap">
<c:set var="contentPath" value="${contentUrl}" scope="page"/>
<c:if test="${not empty pageContext.request.queryString}">
	<c:set var="contentPath" value="${contentUrl}?${pageContext.request.queryString}"/>
</c:if>

<c:choose>
	<c:when test="${pageContext.request.method == 'POST'}">
		${cdr:postImport(pageContext.request, contentUrl)}
	</c:when>
	<c:otherwise>
		<c:catch>
			<c:import url="${contentPath}"/>
		</c:catch>
	</c:otherwise>
</c:choose>
</div>