<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
	<c:param name="target" value="${currentAbsoluteUrl}" />
</c:url>
<div class="contentarea descriptivearea">
	<h2>Login required</h2>
	<p>
		You are not currently logged in.
	</p>
	<p>
		Please <a href="${loginUrl}">log in</a> to access the administrative interface if you already have permissions.  
		If you do not already have permission and would like to request access, please <a href="https://${pageContext.request.serverName}/external?page=contact">contact us</a>.
	</p>
	<p>  
		If you have accessed this page by accident and wish to view our collections instead, they can be accessed via the main <a href="https://${pageContext.request.serverName}/">Digital Collections Repository</a> website.
	</p>
</div>