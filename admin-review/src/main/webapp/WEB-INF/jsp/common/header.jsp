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
<div class="darkest shadowbottom" id="header">
	<div id="header_search" class="fourcol darkest">
		<div class="contentarea">
			<c:if test="${not empty sessionScope.user && not empty sessionScope.user.userName}">
				<div id="username_wrap">Welcome, <c:out value="${sessionScope.user.userName}"/></div>
			</c:if>
		</div>
	</div>
	<div class="threecol dark shadowbottom">
		<div class="contentarea">
			<h1>Carolina Digital Repository</h1>
			
			<a href="${pageContext.request.contextPath}/" id="titlelink"><img src="/static/images/carolinadigitalrepository-trans.png"></a>
			
			<ul id="mainmenu">
				<li>
					<c:set var="homeTabClass" value=""/>
					<c:if test="${contentPage == 'WEB-INF/jsp/frontPage.jsp'}">
						<c:set var="homeTabClass" value="active"/>
					</c:if>
					<a href="${pageContext.request.contextPath}/" class="${homeTabClass}" id="home">Home</a>
				</li>
			</ul>
			<ul class="secondarymenu">
				<li>
					<a href="external?page=cdradmin" target="_blank">Public</a>
				</li>
				<c:choose>
					<c:when test="${not empty sessionScope.user && not empty sessionScope.user.userName}">
						<c:url var="logoutUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Logout">
							<c:param name="return" value="https://sso.unc.edu/idp/logout.jsp?return_url=${currentAbsoluteUrl}" />
						</c:url>
						<li><a href="<c:out value='${logoutUrl}' />" class="login" id="login">Log out</a></li>
					</c:when>
					<c:otherwise>
						<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
							<c:param name="target" value="${currentAbsoluteUrl}" />
						</c:url>
						<li><a href="<c:out value='${loginUrl}' />" class="login" id="login">Login</a></li>
					</c:otherwise>
				</c:choose>
				
			</ul>
		</div>
	</div>
</div>