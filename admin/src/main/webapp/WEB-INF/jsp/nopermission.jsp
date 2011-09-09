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

<title>Log In</title>
<%@ include file="../../html/contents.html"%>

      <li> <a 
            href="<c:url value='/index.jsp'/>">Home </a></li>
      <ul><li> <a 
            href="http://www.lib.unc.edu/forms/cdr/" target="_blank" >Contact Us </a></li>
    </ul>
               
      </div>
      <!-- end leftcolumn -->         
      <div id="content">            
        <p class="breadcrumbs"><a href="<c:url value='/index.jsp'/>">Home</a> > No Permission    
        </p>    
        <h2 class="fontface">No Permission</h2>    

<%
	
    String uri = (String) request.getAttribute("nopermission");

	String hostUrl = (String) request.getAttribute("hostUrl");
	hostUrl = hostUrl.substring(0, hostUrl.lastIndexOf("/cdradmin"));
	
 if(uri != null) {
%>

<p><em><%= uri %></em> is protected content.  To access, you need to have been given a userid and password.</p>
<p><br><a href="http://www.lib.unc.edu/forms/cdr/?subject=Request access to <%= uri %>"\">Request Access</a> or <a href="<%= hostUrl %>/Shibboleth.sso/Login?target=<%= hostUrl %><%= uri %>">Sign In</a><br></p>

<%
	} else {
%>

<p>The Carolina Digital Repository contains some protected content.  To access, you need to have been given a userid and password.</p>
<p><br><a href="http://www.lib.unc.edu/forms/cdr/">Request Access</a><br></p>

<%
	}
%>
<%@ include file="../../html/footer.html"%>
