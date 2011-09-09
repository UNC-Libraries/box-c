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
<title><fmt:message key="submit.collection.heading" /></title>
<LINK REL="stylesheet" HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL="stylesheet" HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />	
</head>
<body>
<div class="records_hdr">
<h1><fmt:message key="submit.collection.heading" /></h1>
</div>
<form:form method="POST" commandName="mediatedSubmitDAO"
	enctype="multipart/form-data">
	<table width="50%" bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tr>
			<td><em><c:out value="${mediatedSubmitDAO.message}" /></em></td>
		</tr>
		<tr>
			<td align="left"><fmt:message
				key="submit.owner.name" /></td><td align="left"><form:select path="ownerPid">
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
			</form:select></td><td><spring:bind path="mediatedSubmitDAO.ownerPid"><FONT color="red"><B><c:out value="${status.errorMessage}"/></B></FONT></spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.submissionCheck" /> </td><td align="left">
			<spring:bind path="mediatedSubmitDAO.submissionCheck">
<input type="hidden" name="_<c:out value="${status.expression}"/>" value="visible" />
<input type="checkbox" name="<c:out value="${status.expression}"/>" value="true" <c:if test="${status.value}">checked</c:if>>
</spring:bind> 
			</td><td><spring:bind path="mediatedSubmitDAO.submissionCheck"><FONT color="red"><B><c:out value="${status.errorMessage}"/></B></FONT></spring:bind></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.collection.path" /> </td><td align="left"><input type="text"
				name="filePath" value="${mediatedSubmitDAO.filePath}" size="60"></td><td><spring:bind path="mediatedSubmitDAO.filePath"><FONT color="red"><B><c:out value="${status.errorMessage}"/></B></FONT></spring:bind></td>
		</tr>
		
		<tr>
			<td align="left"><fmt:message key="submit.metadata" /> </td><td align="left"><input type="file"
				name="metadata" value="${mediatedSubmitDAO.metadata}" size="60"/></td><td><spring:bind path="mediatedSubmitDAO.metadata"><FONT color="red"><B><c:out value="${status.errorMessage}"/></B></FONT></spring:bind></td>
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