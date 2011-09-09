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
<title><fmt:message key="metsubmitbypid.heading"/></title>
<LINK REL=StyleSheet HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />
	
<link type="text/css" href="<c:url value='/css/jquery/ui/jquery-ui.css'/>" rel="stylesheet" />

<script type="text/javascript" src="<c:url value='/js/jquery/jquery.min.js'/>"></script> 
<script type="text/javascript" src="<c:url value='/js/jquery/ui/jquery-ui.min.js'/>"></script> 

<script type="text/javascript">
	$(document).ready( function() {
		$("#virusDate").datepicker( {dateFormat : 'yy-mm-dd'}).val($.datepicker.formatDate('yy-mm-dd', new Date()));
		$("#virusDate").datepicker('option', 'constrainInput', true);
		$("#virusDate").datepicker('option', 'maxDate', '+0m +0w');
	});
</script>
	
</head>
<body>
<div class="path">
<c:forEach items="${metsSubmitByPidDAO.breadcrumbs}" var="breadcrumb">
	<c:out value=" > " />
	<a href="<c:out value="${breadcrumb.path}" />"><c:out value="${breadcrumb.label}" escapeXml="false" /></a>
</c:forEach></div>

<div class="content">
<b><fmt:message key="metsubmitbypid.instructions" /></b>
<br>
<form:form method="POST" commandName="metsSubmitByPidDAO" 	enctype="multipart/form-data">
	<em><c:out value="${metsSubmitByPidDAO.message}" /></em>
<br>
	<table width="40%" bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tbody>
<br>
		<tr>
			<td align="left"><fmt:message key="submit.owner.name" /></td>
			<td align="left"><form:select path="ownerPid">
				<form:option value="">
					<fmt:message key="submit.owner.select" />
				</form:option>
				<form:option value="">
					<fmt:message key="submit.group.separator" />
				</form:option>
				<c:forEach var="item" items="${metsSubmitByPidDAO.groups}"
					varStatus="loop">
					<form:option value="${item.pid}">
						<c:out value="${item.name}" escapeXml="false" />
					</form:option>
				</c:forEach>
				<form:option value="">
					<fmt:message key="submit.user.separator" />
				</form:option>
				<c:forEach var="item" items="${metsSubmitByPidDAO.users}"
					varStatus="loop">
					<form:option value="${item.pid}">
						<c:out value="${item.name}" escapeXml="false" />
					</form:option>
				</c:forEach>
			</form:select></td>
			<td><spring:bind path="metsSubmitByPidDAO.ownerPid">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusCheck" /></td>
			<td align="left"><spring:bind
				path="metsSubmitByPidDAO.virusCheck">
				<input type="hidden" name="_<c:out value="${status.expression}"/>"
					value="visible" />
				<input type="checkbox" name="<c:out value="${status.expression}"/>"
					value="true" <c:if test="${status.value}">checked</c:if>>
			</spring:bind></td>
			<td><spring:bind path="metsSubmitByPidDAO.virusCheck">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusDate" /></td>
			<td align="left"><input type="text" id="virusDate"
				name="virusDate" value="${metsSubmitByPidDAO.virusDate}" size="10" readonly="readonly"></td>
			<td><spring:bind path="metsSubmitByPidDAO.virusDate">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusSoftware" /></td>
			<td align="left"><input type="text" name="virusSoftware"
				value="${metsSubmitByPidDAO.virusSoftware}" size="60"></td>
			<td><spring:bind path="metsSubmitByPidDAO.virusSoftware">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.mets" /></td>
			<td align="left"><input type="file" name="file"
				value="${metsSubmitByPidDAO.file}" size="60" /></td>
			<td><spring:bind path="metsSubmitByPidDAO.file">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr><td align="left"><fmt:message key="submit.ingest.path" /></td>
		<td>
<c:forEach items="${metsSubmitByPidDAO.breadcrumbs}" var="breadcrumb">
	<c:out value=" > " />
	<c:out value="${breadcrumb.label}" escapeXml="false" />
</c:forEach></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.filepath.additional" /></td>
			<td align="left"><input id="filePath" type="text"
				name="filePath" value="${metsSubmitByPidDAO.filePath}" size="60"></td>
			<td><spring:bind path="metsSubmitByPidDAO.filePath">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		</tbody>
	</table>
	<center><input type="submit" alignment="center" value="<fmt:message key="submit.submit"/>"/></center>
	</form:form>
	<br><br>
<table>
<tbody>
</tbody>
<c:if test="${not empty metsSubmitByPidDAO.paths}">
			<c:forEach items="${metsSubmitByPidDAO.paths}" var="entry">
				<tr><td align="left"><a href="<c:out value="${entry.path}" />"><c:out value="${entry.label}" escapeXml="false" /></a></td>
				</tr>
				
			</c:forEach>			
</c:if>
</table>

<form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form>
</div>
</body>
</html>
