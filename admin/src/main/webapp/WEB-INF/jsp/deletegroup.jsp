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
<title><fmt:message key="um.group.delete.heading" /></title>
<LINK REL=StyleSheet HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />	
</head>
<body>
<div class="records_hdr">
<h1><fmt:message key="um.group.delete.heading" /></h1>
</div>
<form action="deletegroup" commandName="userGroupDAO">
<table width="40%" bgcolor="f8f8ff" border="0" cellspacing="0"
	cellpadding="5">
	<tr><td><em><c:out value="${userGroupDAO.message}"/></em></td></tr>
	<tr>
		<td alignment="right" width="20%"><fmt:message key="um.user.name"/></td>
			<td width="20%" alignment="right">
<form:select path="userGroupDAO.pid">
            <option value=""><fmt:message key="um.group.delete.select" /></option>            
			<c:forEach var="item" items="${userGroupDAO.groups}" varStatus="loop">
					<form:option value="${item.pid}">
						<c:out value="${item.name}" escapeXml="false" /></form:option>
			</c:forEach>
		</form:select>
		</td>			
	</tr>
</table>
<br>
<br>
<input type="submit" alignment="center" value=<fmt:message key="um.delete"/>></form>
<form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form>
</body>
</html>