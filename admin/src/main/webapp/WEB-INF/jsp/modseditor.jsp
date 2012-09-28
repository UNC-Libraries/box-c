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

--%><%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%><%--
--%><!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <%@ include file="include.jsp"%>
  <%@ include file="../../html/head.html"%>
  <link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
  <title><fmt:message key="updateobject.heading"/></title>

  <link rel="stylesheet" href="../../css/jquery/ui/jquery-ui.css" type="text/css" media="all" />
  <link rel="stylesheet" href="<c:url value='/css/xml_editor_style.css'/>" type="text/css" />
  <script type="text/javascript" src="../../js/jquery/jquery.min.js"></script>
  <script type="text/javascript" src="../../js/jquery/ui/jquery-ui.min.js"></script>
  <script src="/cdradmin/js/jquery.xmlns.js"></script>
  <script src="/cdradmin/js/jquery.modseditor.js"></script>
  <script src="/cdradmin/js/modsattributes.js"></script>
  <script src="/cdradmin/js/modselements.js"></script>
  <script src="/cdradmin/js/ace/src-min/ace.js"></script>
  <script src="/cdradmin/js/vkbeautify.0.98.01.beta.js"></script>

</head>
<body>
<%@ include file="../../html/admincontents.html"%>
<div id="content">
	<div id="mods_editor">
	</div>
</div>
<%@ include file="../../html/footer.html"%>
<script>
var modsDocument = null;

$(document).ready(function(){
	$("#mods_editor").modsEditor({
		ajaxOptions : {
			modsRetrievalPath : "/cdradmin/ir/admin/ajax/mods",
			modsRetrievalParams : {'pid' : '<c:out value="${param.pid}"/>'},
			modsUploadPath : "/cdradmin/ir/admin/updatemods?pid=<c:out value='${param.pid}'/>"
		},
		documentTitle : '<%= ((String)request.getAttribute("objectLabel")).replaceAll("'","\\\\'").replaceAll("[\u0000-\u001f]", "") %>'
	});
});

</script>
</body>
</html>