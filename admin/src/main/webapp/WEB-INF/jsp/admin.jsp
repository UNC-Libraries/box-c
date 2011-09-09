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
<%@ include file="include.jsp"%>
<%@ include file="../../html/head.html"%>
<title><fmt:message key="admin.heading" /></title>
<%@ include file="../../html/admincontents.html"%>
      <div id="content">            
        <p class="breadcrumbs"><a href="<c:url value='/index.jsp'/>">Home</a> > Administration    
        </p>    
        <h2 class="fontface">Administration</h2>    
        <p>The CDR user manual is located <a href="https://intranet.lib.unc.edu:82/trac/cdr/wiki/UserManual">here</a>.</p>
		<br>
        <p>The users and groups created and deleted on these pages are for ownership in the CDR.  To work with users and groups for access control, you will need to work with <a href="https://intranet.lib.unc.edu/wikis/staff/index.php/Adding_a_group_to_Grouper">Grouper</a>.</p>



<%@ include file="../../html/footer.html"%>
