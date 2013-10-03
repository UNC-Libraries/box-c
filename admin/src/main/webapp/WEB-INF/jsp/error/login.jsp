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
		If you have accessed this page by accident and wish to view our collections instead, they can be accessed via the main <a href="https://${pageContext.request.serverName}/">Carolina Digital Repository</a> website. 
	</p>
</div>