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
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="header.jsp"%>

<html>
<head>
<link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
<title><fmt:message key="moveobject.heading"/></title>
<LINK REL="stylesheet" HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL="stylesheet" HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />
</head>
<body>
<c:if test="${empty moveObjectDAO.breadcrumbs}">
	<c:out value="${moveobject.submitmessage}" escapeXml="false" />
</c:if>
<div class="path">
<c:forEach items="${moveObjectDAO.breadcrumbs}" var="breadcrumb">
	<c:out value=" > " />
	<a href="<c:out value="${breadcrumb.path}" />"><c:out value="${breadcrumb.label}" escapeXml="false" /></a>
</c:forEach></div>

<div class="content">
<c:if test="${not empty moveObjectDAO.paths}">
<form:form method="POST" commandName="moveObjectDAO"
	enctype="multipart/form-data">
	<table id="children" style="margin-top: 0pt;"
		xmlns:mods="http://www.loc.gov/mods/v3" align="center">
		<tbody>
		<c:if test="${(fn:length(moveObjectDAO.breadcrumbs) == 1) && (moveObjectDAO.groupName eq 'parent')}">
				<tr><td><input type="${moveObjectDAO.groupType}" name="${moveObjectDAO.groupName}" value="${moveObjectDAO.collectionPid}"/>&nbsp;<fmt:message key='movetoparent.collectionLabel'/></td>
				</tr>
	    </c:if>						
			<c:forEach items="${moveObjectDAO.paths}" var="entry">
				<tr><td><input type="${moveObjectDAO.groupType}" name="${moveObjectDAO.groupName}" value="${entry.pid}"/>&nbsp;<a href="<c:out value="${entry.path}" />"><c:out value="${entry.label}" escapeXml="false" /></a></td>
				</tr>
			</c:forEach>			
		</tbody>
	</table>
	<center><input type="submit" alignment="center" value="<fmt:message key="moveobject.submit"/>"/></center>
	</form:form>
</c:if>
<form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form>
</div>
</body>
</html>