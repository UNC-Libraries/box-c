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
<title><fmt:message key="fixityreplication.heading"/></title>
<LINK REL=StyleSheet HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />
	
<link type="text/css" href="<c:url value='/css/jquery/ui/jquery-ui.css'/>" rel="stylesheet" />

<script type="text/javascript" src="<c:url value='/js/jquery/jquery.min.js'/>"></script> 
<script type="text/javascript" src="<c:url value='/js/jquery/ui/jquery-ui.min.js'/>"></script> 
	
</head>
<body>

<div class="content">
<b><fmt:message key="fixityreplication.instructions" /></b>
<br>
<form:form method="POST" commandName="fixityReplicationDAO" 	enctype="multipart/form-data">
  <p>
	  <em><c:out escapeXml="false" value="${fixityReplicationDAO.message}" /></em>
  </p>

<div style="float: right">
	<table bgcolor="f8f8ff" border="0" cellspacing="0"
		cellpadding="5">
		<tbody>
		<tr>
			<td align="left"><fmt:message key="fixityreplication.replication.good" /></td>
			<td align="left"><input type="file" name="goodReplicationFile"
				value="${fixityReplicationDAO.goodReplicationFile}" size="60" /></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="fixityreplication.replication.bad" /></td>
			<td align="left"><input type="file" name="badReplicationFile"
				value="${fixityReplicationDAO.badReplicationFile}" size="60" /></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="fixityreplication.fixity.good" /></td>
			<td align="left"><input type="file" name="goodFixityFile"
				value="${fixityReplicationDAO.goodFixityFile}" size="60" /></td>
		</tr>
		<tr>
			<td align="left"><fmt:message key="fixityreplication.fixity.bad" /></td>
			<td align="left"><input type="file" name="badFixityFile"
				value="${fixityReplicationDAO.badFixityFile}" size="60" /></td>
		</tr>
		</tbody>
	</table>
	<center><input type="submit" alignment="center" value="<fmt:message key="submit.submit"/>"/></center>
	</form:form>
	<center><form method="LINK" action="<c:url value='/ir/admin/'/>"><input type="submit" value='<fmt:message key="um.return.to.admin.page"/>'></form></center>
</div>
</div>
</body>
</html>
