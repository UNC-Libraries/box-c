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
<div class="darkest shadowbottom" id="header">
	<div class="threecol dark shadowbottom">
		<div class="contentarea">
			<h1>Carolina Digital Repository</h1>
			
			<a href="${pageContext.request.contextPath}/" id="titlelink"><img src="/static/images/carolinadigitalrepository.png"></a>
			
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
			<ul class="secondarymenu">
				<c:if test="${cdr:contains(requestScope.accessGroupSet, accessGroupConstants.ADMIN_GROUP)}">
					<li>
						<a href="external?page=cdradmin" target="_blank">Admin</a>
					</li>
				</c:if>
				<c:if test="${requestScope.hasAdminViewPermission}">
					<li>
						<a href="/admin/" target="_blank">Review</a>
					</li>
				</c:if>
				<c:choose>
					<c:when test="${not empty pageContext.request.remoteUser}">
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
	<div class="fourcol darkest">
		<div class="contentarea">
			<c:if test="${not empty pageContext.request.remoteUser}">
				<div id="username_wrap">Welcome, <c:out value="${pageContext.request.remoteUser}"/></div>
			</c:if>
			<c:if test="${contentPage != 'frontPage.jsp'}">
				<form class="right" method="get" action="basicSearch" id="hsearch_form">
					<input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}"/>
					<div id="hsearch_inputwrap">
						<input name="query" type="text" id="hsearch_text" placeholder="Search all collections">
						<input type="submit" value="Go" id="hsearch_submit">
					</div>
				</form>
				<ul class="secondarymenu">
					<li>
						<a href="advancedSearch" class="searchlink" id="advsearch">Advanced Search</a>
					</li>
				</ul>
			</c:if>
		</div>
	</div>
</div>