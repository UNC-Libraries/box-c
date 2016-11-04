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
<div class="dark shadowbottom" id="header">
	<ul id="topbar">
		<c:choose>
			<c:when test="${not empty pageContext.request.remoteUser}">
				<c:url var="logoutUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Logout">
					<c:param name="return" value="https://sso.unc.edu/idp/logout.jsp?return_url=${currentAbsoluteUrl}" />
				</c:url>
				<li class="topbar-menu-option" id="login"><a href="<c:out value='${logoutUrl}' />" class="login" id="login">Log out</a></li>
			</c:when>
			<c:otherwise>
				<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
					<c:param name="target" value="${currentAbsoluteUrl}" />
				</c:url>
				<li class="topbar-menu-option"><a href="<c:out value='${loginUrl}' />" class="login" id="login">Login</a></li>
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
				<a href="${accessBaseUrl}/" data-base-href="${accessBaseUrl}/" id="public_ui_link" target="_blank">Public</a>
			</li>
		</c:if>
		<li class="topbar-menu-option">
		<a href="http://blogs.lib.unc.edu/cdr/index.php/contact-us/">Contact</a></li>
		<li class="topbar-menu-option">
			<a href="http://blogs.lib.unc.edu/cdr/">About</a>
		</li>
		<c:if test="${not empty pageContext.request.remoteUser}">
			<li id="username_wrap">Welcome, <c:out value="${pageContext.request.remoteUser}"/></li>
		</c:if>
	</ul>
	<div class="dark shadowbottom">
		<div class="cdr-header">
			<h1 id="cdr-logo"><a href="${pageContext.request.contextPath}/" id="titlelink"><span class="dark-title">CAROLINA</span> <span class="light-title">DIGITAL</span> <span class="dark-title">REPOSITORY</span><span class="sub-title">Administration</span></a></h1>
			
			<ul id="mainmenu">
				<li>
					<c:set var="tabClass"><c:if test="${requestScope.queryMethod == 'list' || requestScope.queryMethod == 'search'}">active</c:if></c:set>
					<a class="${tabClass}" href="${pageContext.request.contextPath}/list">Browse</a>
				</li>
				<li>
					<c:set var="tabClass"><c:if test="${requestScope.queryMethod == 'trash'}">active</c:if></c:set>
					<a class="${tabClass}" href="${pageContext.request.contextPath}/trash">Trash</a>
				</li>
				<li>
					<c:set var="tabClass"><c:if test="${contentPage == 'report/statusMonitor.jsp'}">active</c:if></c:set>
					<a href="statusMonitor" id="menu_status_monitor" class="${tabClass}">Status Monitor</a>
				</li>
				<c:if test="${sessionScope.accessLevel.highestRole.predicate == 'administrator'}">
					<li>
						<c:set var="tabClass"><c:if test="${contentPage == 'report/performanceMonitor.jsp'}">active</c:if></c:set>
						<a href="performanceMonitor" class="${tabClass}">Metrics</a>
					</li>
				</c:if>
				<li>
					<c:set var="tabClass"><c:if test="${contentPage == 'collector/listBins.jsp'}">active</c:if></c:set>
					<a href="collector" class="${tabClass}">Deposit Collectors</a>
				</li>
			</ul>
		</div>
	</div>
</div>