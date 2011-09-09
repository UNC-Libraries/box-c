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
<title><fmt:message key="submit.self.heading" /></title>
<LINK REL="StyleSheet" HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL="StyleSheet" HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />	
</head>
<body>
<div class="records_hdr">
<h1><fmt:message key="submit.self.heading" /></h1>
</div>
<form:form method="POST" commandName="selfSubmitDAO"
	enctype="multipart/form-data">
	<table width="40%" bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tr>
			<td><c:out value="${selfSubmitDAO.message}" /></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.filepath" /> </td><td align="left"><input type="text"
				name="filePath" value="${mediatedSubmitDAO.filePath}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.file" /> </td><td align="left"><input type="file"
				name="file" value="${mediatedSubmitDAO.file}"/></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.checksum" /> </td><td align="left"><input type="text"
				name="checksum" value="${mediatedSubmitDAO.checksum}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.metadata" /> </td><td align="left"><input type="file"
				name="metadata" value="${mediatedSubmitDAO.metadata}"/></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.creator" /> </td><td align="left"><input type="text"
				name="creator" value="${selfSubmitDAO.creator}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.date.created" /> </td><td align="left"><input type="text"
				name="date" value="${selfSubmitDAO.date}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.language" /> </td><td align="left"><input type="text"
				name="language" value="${selfSubmitDAO.language}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.abstract" /> </td><td align="left"><input type="text"
				name="abstract" value="${selfSubmitDAO.abstract}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.subject" /> </td><td align="left"><input type="text"
				name="subject" value="${selfSubmitDAO.subject}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.description.physical" /> </td><td align="left"><input type="text"
				name="physical" value="${selfSubmitDAO.physical}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.genre" /> </td><td align="left"><input type="text"
				name="genre" value="${selfSubmitDAO.genre}"></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="submit.rights" /> </td><td align="left"><input type="text"
				name="rights" value="${selfSubmitDAO.rights}"></td>
		</tr>
	</table>
	<br>
	<br>
	<input type="submit" alignment="center"
		value=<fmt:message key="submit.submit"/>>
</form:form>
</body>
</html>