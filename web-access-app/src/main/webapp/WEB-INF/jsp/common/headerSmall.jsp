<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>

<header>
    <div class="logo-row-small">
        <div class="logo-small container">
            <a href="/">
                <img src="static/front/university-libraries-logo.png" alt="University Libraries Logo">
                <h1>Digital Collections Repository</h1>
            </a>
            <span class="info-btns">
                <a href="${contactUrl}">Contact Us</a>
                <c:choose>
                    <c:when test="${not empty cdr:getUsername()}">
                        <c:url var="logoutUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Logout">
                            <c:param name="return" value="https://sso.unc.edu/idp/logout.jsp?return_url=${currentAbsoluteUrl}" />
                        </c:url>
                        <a href="<c:out value='${logoutUrl}' />"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
                    </c:when>
                    <c:otherwise>
                        <c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
                            <c:param name="target" value="${currentAbsoluteUrl}" />
                        </c:url>
                        <a href="<c:out value='${loginUrl}' />"><i class="fas fa-user"></i>&nbsp;&nbsp;Login</a>
                    </c:otherwise>
                </c:choose>
            </span>
        </div>
    </div>
    <nav class="menu-row-small navbar" role="navigation">
        <div class="container">
            <div class="navbar-brand">
                <a role="button" id="navbar-burger" class="navbar-burger burger" aria-label="menu" aria-expanded="false" data-target="navbar">
                    <span aria-hidden="true"></span>
                    <span aria-hidden="true"></span>
                    <span aria-hidden="true"></span>
                </a>
            </div>
            <div id="navbar" class="menu navbar-menu">
                <a href="collections" class="navbar-item">Browse Collections</a>
                <a href="aboutRepository" class="navbar-item">What's Here?</a>
                <c:if test="${sessionScope.accessLevel != null && sessionScope.accessLevel.viewAdmin}">
                    <c:choose>
                        <c:when test="${not empty resultResponse && not empty resultResponse.selectedContainer}">
                            <s:eval var="jumpToAdmin" expression=
                                "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', resultResponse.selectedContainer.id)" />
                        </c:when>
                        <c:when test="${not empty briefObject && briefObject.resourceType == 'File'}">
                            <s:eval var="jumpToAdmin" expression=
                                "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', briefObject.ancestorPathFacet.searchKey)" />
                        </c:when>
                        <c:when test="${not empty briefObject}">
                            <s:eval var="jumpToAdmin" expression=
                                "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'list', briefObject.id)" />
                        </c:when>
                        <c:otherwise>
                            <c:set var="jumpToAdmin" value="${adminBaseUrl}" />
                        </c:otherwise>
                    </c:choose>
                    <a href="${jumpToAdmin}" class="navbar-item" target="_blank">Admin</a>
                </c:if>
                <a class="navbar-item navbar-display" href="${contactUrl}">Contact Us</a>
                <c:choose>
                    <c:when test="${not empty cdr:getUsername()}">
                        <c:url var="logoutUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Logout">
                            <c:param name="return" value="https://sso.unc.edu/idp/logout.jsp?return_url=${currentAbsoluteUrl}" />
                        </c:url>
                        <a class="navbar-item navbar-display" href="<c:out value='${logoutUrl}' />"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
                    </c:when>
                    <c:otherwise>
                        <c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
                            <c:param name="target" value="${currentAbsoluteUrl}" />
                        </c:url>
                        <a class="navbar-item navbar-display" href="<c:out value='${loginUrl}' />"><i class="fas fa-user"></i>&nbsp;&nbsp;Login</a>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="search-row">
                <div class="search">
                    <form method="get" action="basicSearch" class="search">
                        <input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}">
                        <label for="hsearch_text">Search the Digital Collections Repository</label>
                        <input name="query" type="text" id="hsearch_text" placeholder="Search all collections">
                        <button type="submit" class="button">Search</button>
                    </form>
                    <a class="navbar-item" href="advancedSearch">Advanced Search</a>
                </div>
            </div>
        </div>
    </nav>
    <script type="text/javascript" src="/static/js/public/mobileMenu"></script>
</header>