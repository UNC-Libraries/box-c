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
<%@ include file="header.jsp"%>

<html>
<head>
<link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
<title><fmt:message key="submit.mets.heading" /></title>

<LINK REL="stylesheet" HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL="stylesheet" HREF="<c:url value='/css/ir_style.css'/>"
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
<div class="records_hdr">
<h1><fmt:message key="submit.mets.heading" /></h1>
</div>
<form:form method="POST" commandName="mediatedSubmitDAO"
	enctype="multipart/form-data">
	<em><c:out value="${mediatedSubmitDAO.message}" /></em>
	<table width="50%" bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tr>
			<td align="left"><fmt:message key="submit.owner.name" /></td>
			<td align="left"><form:select path="ownerPid">
				<form:option value="">
					<fmt:message key="submit.owner.select" />
				</form:option>
				<form:option value="">
					<fmt:message key="submit.group.separator" />
				</form:option>
				<c:forEach var="item" items="${mediatedSubmitDAO.groups}"
					varStatus="loop">
					<form:option value="${item.pid}">
						<c:out value="${item.name}" escapeXml="false" />
					</form:option>
				</c:forEach>
				<form:option value="">
					<fmt:message key="submit.user.separator" />
				</form:option>
				<c:forEach var="item" items="${mediatedSubmitDAO.users}"
					varStatus="loop">
					<form:option value="${item.pid}">
						<c:out value="${item.name}" escapeXml="false" />
					</form:option>
				</c:forEach>
			</form:select></td>
			<td><spring:bind path="mediatedSubmitDAO.ownerPid">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusCheck" /></td>
			<td align="left"><spring:bind
				path="mediatedSubmitDAO.virusCheck">
				<input type="hidden" name="_<c:out value="${status.expression}"/>"
					value="visible" />
				<input type="checkbox" name="<c:out value="${status.expression}"/>"
					value="true" <c:if test="${status.value}">checked</c:if>>
			</spring:bind></td>
			<td><spring:bind path="mediatedSubmitDAO.virusCheck">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusDate" /></td>
			<td align="left"><input type="text" id="virusDate"
				name="virusDate" value="${mediatedSubmitDAO.virusDate}" size="10" readonly="readonly"></td>
			<td><spring:bind path="mediatedSubmitDAO.virusDate">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.virusSoftware" /></td>
			<td align="left"><input type="text" name="virusSoftware"
				value="${mediatedSubmitDAO.virusSoftware}" size="60"></td>
			<td><spring:bind path="mediatedSubmitDAO.virusSoftware">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.parentpid" /></td>
			<td align="left"><input id="parentPid" type="text"
				name="filePath" value="${mediatedSubmitDAO.parentPid}" size="60"></td>
			<td><spring:bind path="mediatedSubmitDAO.parentPid">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.filepath.additional" /></td>
			<td align="left"><input id="filePath" type="text"
				name="filePath" value="${mediatedSubmitDAO.filePath}" size="60"></td>
			<td><spring:bind path="mediatedSubmitDAO.filePath">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.mets" /></td>
			<td align="left"><input type="file" name="file"
				value="${mediatedSubmitDAO.file}" size="60" /></td>
			<td><spring:bind path="mediatedSubmitDAO.file">
				<FONT color="red"><B><c:out
					value="${status.errorMessage}" /></B></FONT>
			</spring:bind></td>
		</tr>
	</table>
	<br>
	<br>
	<input type="submit" alignment="center"
		value=<fmt:message key="submit.submit"/>>
</form:form>
<form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form>
</body>
</html>