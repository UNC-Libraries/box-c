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
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<header class="">
	<div class="logo-row">
		<div class="logo container">
			<a href="">
				<img src="static/images/university-libraries-logo.png" alt="University Libraries Logo">
				<h1>Digital Collections Repository</h1>
			</a>
		</div>
	</div>
	<nav class="menu-row navbar" role="navigation">
		<div class="container">
			<div class="navbar-brand">
				<a role="button" class="navbar-burger burger" aria-label="menu" aria-expanded="false" data-target="navbar">
					<span aria-hidden="true"></span>
					<span aria-hidden="true"></span>
					<span aria-hidden="true"></span>
				</a>
			</div>
			<div id="navbar" class="menu navbar-menu">
				<div class="navbar-start">
					<a href="/" class="navbar-item">Home</a>
					<a class="navbar-item">Browse Collections</a>
					<a href="https://blogs.lib.unc.edu/cdr/" class="navbar-item">What's Here?</a>
					<a href="https://blogs.lib.unc.edu/cdr/index.php/contact-us/" class="navbar-item">Contact Us</a>
				</div>
				<div class="navbar-end">
					<c:choose>
						<c:when test="${not empty pageContext.request.remoteUser}">
							<c:url var="logoutUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Logout">
								<c:param name="return" value="https://sso.unc.edu/idp/logout.jsp?return_url=${currentAbsoluteUrl}" />
							</c:url>
							<a href="<c:out value='${logoutUrl}' />" class="navbar-item"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
						</c:when>
						<c:otherwise>
							<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
								<c:param name="target" value="${currentAbsoluteUrl}" />
							</c:url>
							<a href="<c:out value='${loginUrl}' />" class="navbar-item"><i class="fas fa-user"></i>&nbsp;&nbsp;Login</a>
						</c:otherwise>
					</c:choose>
					<c:if test="${sessionScope.accessLevel != null && sessionScope.accessLevel.viewAdmin}">
						<c:choose>
							<c:when test="${not empty resultResponse && not empty resultResponse.selectedContainer}">
								<c:set var="jumpToAdmin" value="list/${resultResponse.selectedContainer.id}" />
							</c:when>
							<c:when test="${not empty briefObject && briefObject.resourceType == 'File'}">
								<c:set var="jumpToAdmin" value="list/${briefObject.ancestorPathFacet.searchKey}" />
							</c:when>
							<c:when test="${not empty briefObject}">
								<c:set var="jumpToAdmin" value="list/${briefObject.id}" />
							</c:when>
						</c:choose>
						<li class="topbar-menu-option">
							<a href="${adminBaseUrl}/${jumpToAdmin}" class="navbar-item" target="_blank">Admin</a>
						</li>
					</c:if>
				</div>
			</div>
		</div>
	</nav>
	<!-- Remove this block for other pages so that menu collapses down with search bar -->
	<div class="banner-row">
		<div class="banner container">
			<h2>Explore materials from Wilson Special Collections Library</h2>
			<a class="button is-link is-large">Begin your exploration</a>
		</div>
	</div>
	<!-- End remove block -->
	<div class="search-row">
		<div class="search container">
			<form method="get" action="basicSearch" class="search">
				<input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}">
				<label for="search">Search the Carolina Digital Repository</label>
				<input type="text" name="search" id="search">
				<button class="button">Search</button>
				<a href="advancedSearch">Advanced Search</a>
			</form>
		</div>
	</div>
</header>