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
				<li>
					<c:set var="homeTabClass" value=""/>
					<c:if test="${contentPage == 'WEB-INF/jsp/frontPage.jsp'}">
						<c:set var="homeTabClass" value="active"/>
					</c:if>
					<a href="${pageContext.request.contextPath}/" class="${homeTabClass}" id="home">Home</a>
				</li>
				<li>
					<c:set var="browseTabClass" value=""/>
					<c:if test="${resultType == 'collectionBrowse' || resultType == 'departmentBrowse'}">
						<c:set var="browseTabClass" value="active"/>
					</c:if>
					<c:url value="search" scope="page" var="browseCollectionUrl">
						<c:param name="${searchSettings.searchStateParams['RESOURCE_TYPES']}" value="${cdr:join(searchSettings.defaultCollectionResourceTypes, ',')}"/>
					</c:url>
					<a href="<c:out value='${browseCollectionUrl}' />" id="browseall" class="${browseTabClass}">Browse</a>
					<ul class="submenu">
						<li><a href="<c:out value='${browseCollectionUrl}' />">Browse Collections</a></li>
						<li><a href="browseDepartments">Browse Departments</a></li>
					</ul>	
				</li>
				<li><a href="external?page=about.about" id="about">About</a>
					<ul class="submenu">
						<c:forEach var="aboutPage" items="${externalContentSettings.aboutPages}">
							<c:set var="aboutTarget" scope="page">
								<c:choose>
									<c:when test="${not empty aboutPage.target}">target="_blank"</c:when>
									<c:otherwise></c:otherwise>
								</c:choose>
							</c:set>
							<li><a href="external?page=${aboutPage.key}" ${aboutTarget}>${aboutPage.label}</a></li>
						</c:forEach>
					</ul>
				</li>
				<c:url var="contactUrl" scope="page" value="external">
					<c:param name="page" value="contact" />
					<c:choose>
						<c:when test="${param.page == 'contact'}">
							<c:param name="refer" value="${param.refer}"/>
						</c:when>
						<c:otherwise>
							<c:param name="refer" value="${currentAbsoluteUrl}"/>
						</c:otherwise>
					</c:choose>
				</c:url>
				<li><a href="<c:out value='${contactUrl}'/>" id="contact">Contact</a></li>
			</ul>
			<ul class="secondarymenu">
				<c:if test="${cdr:contains(requestScope.accessGroupSet, accessGroupConstants.ADMIN_GROUP)}">
					<li>
						<a href="external?page=cdradmin" target="_blank">Admin</a>
					</li>
				</c:if>
				<c:choose>
					<c:when test="${not empty requestScope.accessGroupSet}">
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
			<c:if test="${not empty sessionScope.user && not empty sessionScope.user.userName}">
				<div id="username_wrap">Welcome, <c:out value="${sessionScope.user.userName}"/></div>
			</c:if>
			<c:if test="${contentPage != 'WEB-INF/jsp/frontPage.jsp'}">
				<form class="right clear_on_submit_without_focus" method="get" action="basicSearch" id="hsearch_form">
					<input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}"/>
					<div id="hsearch_inputwrap">
						<input name="query" type="text" id="hsearch_text" class="clear_on_first_focus" value="Search all collections">
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