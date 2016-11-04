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

<div class="darkest fluid-cap-container" id="header">
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
				<a href="${adminBaseUrl}/${jumpToAdmin}" target="_blank">Admin</a>
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
	<div class="darkest fluid-cap-highlight">
		<div class="fluid-cap-contents">
			<div class ="fluid-cap-right-wrap darkest">
				<div class="fluid-cap-left-wrap darkest">
					<div class="twocol fluid-cap-left">
					
		<div class="cdr-header">
			<h1 id="cdr-logo"><a href="/" id="titlelink"><span class="white-title">CAROLINA</span> <span class="blue-title">DIGITAL</span> <span class="white-title">REPOSITORY</span></a></h1>
			
			<ul id="mainmenu">
				<c:set var="referUrl">
					<c:choose>
						<c:when test="${not empty param.refer}">
							${param.refer}
						</c:when>
						<c:otherwise>
							${currentAbsoluteUrl}
						</c:otherwise>
					</c:choose>
				</c:set>
			
				<c:forEach items="${headerMenuSettings.menuRoot.subMenus}" var="menuEntry">
					<li>
						<c:set var="activeClass">
							<c:if test="${requestScope.menuId == menuEntry.key}">
								active
							</c:if>
						</c:set>
						<c:set var="menuTarget">
							<c:choose>
								<c:when test="${not empty menuEntry.value.target}">target="${menuEntry.value.target}"</c:when>
								<c:otherwise></c:otherwise>
							</c:choose>
						</c:set>
						<c:if test="${not empty menuEntry.value.url}">
							<c:url var="menuUrl" value="${menuEntry.value.url}">
								<c:if test="${menuEntry.value.includeReferer}">
									<c:param name="refer" value="${referUrl}"/>
								</c:if>
							</c:url>
							<a href="<c:out value='${menuUrl}' />" class="${activeClass}" ${menuTarget}>${menuEntry.value.label}</a>
						</c:if>
						<c:if test="${not empty menuEntry.value.subMenus}">
							<ul class="submenu">
								<c:forEach items="${menuEntry.value.subMenus}" var="subEntry">
									<li>
										<c:set var="menuTarget">
											<c:choose>
												<c:when test="${not empty subEntry.value.target}">target="${subEntry.value.target}"</c:when>
												<c:otherwise></c:otherwise>
											</c:choose>
										</c:set>
										<c:url var="menuUrl" value="${subEntry.value.url}">
											<c:if test="${subEntry.value.includeReferer}">
												<c:param name="refer" value="${referUrl}"/>
											</c:if>
										</c:url>
										<a href="<c:out value='${menuUrl}' />" ${menuTarget}>${subEntry.value.label}</a>
									</li>
								</c:forEach>
							</ul>
						</c:if>
					</li>
				</c:forEach>
			</ul>
		</div>
	</div>
	<div id="searchoptions">
		
		<div class="contentarea">
		<div id="searchoptions-bottom">
			<div id="advancedsearch">
			<a href="advancedSearch">Advanced Search</a>
			</div>
				<form class="right clear_on_submit_without_focus" method="get" action="basicSearch" id="hsearch_form">
						<input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}">
						<div id="hsearch_inputwrap">
						   <input name="query" type="text" id="hsearch_text" placeholder="Search all collections">
						   <input type="submit" value="Go" id="hsearch_submit">
						</div>
				 </form>
		</div>
	</div>
</div>
		</div>
	</div>
</div>
</div>
</div>