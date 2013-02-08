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
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
<title><fmt:message key="updateobject.heading"/></title>
<LINK REL=StyleSheet HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />
</head>
<body>
<div class="path">
<c:forEach items="${updateObjectDAO.breadcrumbs}" var="breadcrumb">
	<c:out value=" > " />
	<a href="<c:out value="${breadcrumb.path}" />"><c:out value="${breadcrumb.label}" escapeXml="false" /></a>
</c:forEach></div>

<div class="content">
<b><fmt:message key="update.instructions" /></b>
<form:form method="POST" commandName="updateObjectDAO" 	enctype="multipart/form-data">
	<table width="40%" bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tbody>
<c:if test="${(fn:length(updateObjectDAO.breadcrumbs) > 1)}">
		<tr>
				<tr><td align="left"><fmt:message key="submit.metadata" /></td><td align="left">
			<input type="file" name="metadata.sourceFile" value="${updateObjectDAO.metadata.sourceFile}"/></td>
				</tr>
		<tr>
			<td align="left"><fmt:message key="update.metadata.checksum" /> </td><td align="left"><input type="text"
				name="checksum" value="${mediatedSubmitDAO.checksum}"></td>
		</tr>
		<tr><td>&nbsp;</td></tr>
</c:if>
<c:if test="${not empty updateObjectDAO.files}">
			<c:forEach items="${updateObjectDAO.files}" var="datastream" varStatus="status">
		<tr><td align="left"><fmt:message key="submit.label" /> </td><td align="left"><spring:bind path="updateObjectDAO.files[${status.index}].label"><input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/></spring:bind></td></tr>
		<tr><td align="left"><fmt:message key="submit.file" /> </td><td align="left"><spring:bind path="updateObjectDAO.files[${status.index}].sourceFile"><input type="file" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/></spring:bind></td></tr>
		<tr><td align="left"><fmt:message key="update.file.checksum" /> </td><td align="left"><spring:bind path="updateObjectDAO.files[${status.index}].checksum"><input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/></spring:bind></td></tr>
		<tr><td>&nbsp;</td></tr>
			</c:forEach>			
</c:if>
<br>
<c:if test="${not empty updateObjectDAO.paths}">
			<c:forEach items="${updateObjectDAO.paths}" var="entry">
				<tr><td align="left"><a href="<c:out value="${entry.path}" />"><c:out value="${entry.label}" escapeXml="false" /></a></td>
				</tr>
				
			</c:forEach>			
</c:if>
		</tbody>
	</table>
	<!-- <center><input type="submit" alignment="center" name="submit" value="<fmt:message key="updateobject.editac"/>"/></center> -->
	<center><input type="submit" alignment="center" name="submit" value="<fmt:message key="updateobject.submit"/>"/></center>
	</form:form>
<form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form>
</div>
</body>
</html>
