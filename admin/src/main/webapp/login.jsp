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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="html/head.html"%>

<title>Log In</title>
<%@ include file="html/contents.html"%>

      <li> <a 
            href="/index.jsp">Home </a></li>
      <li> <a class='active' 
            href="/ir/info/Collections">Collections </a></li>

      <li> <a 
              href="/about.jsp">About the Repository </a>
      
      
      <ul><li> <a href="/faq.jsp">Repository FAQ</a><li>
      <li> <a href="/policies.jsp">Policies &amp; Guidelines</a><li>
      <li> <a href="">Publications &amp; Presentations</a><li></ul>      
      </li>

      <li> <a 
            href="http://www.lib.unc.edu/forms/cdr/" target="_blank" >Contact Us </a></li>
    </ul>
               
      </div>
      <!-- end leftcolumn -->         
      <div id="content">            
        <p class="breadcrumbs"><a href="/index.jsp">Home</a> > Log in    
        </p>    
        <h2 class="fontface">Log in</h2>    

<c:if test="${not empty param.error}">
  <br>
  <font color="red">
  Login failed.  Perhaps you need to request a userid and password to access this content.
  </font> 
  <br>  
</c:if>

<%

/* java.util.Enumeration keys = session.getAttributeNames();
while (keys.hasMoreElements())
{
  String key = (String)keys.nextElement();
  out.println(key + ": " + session.getValue(key) + "<br>");
}
*/

    org.springframework.security.web.savedrequest.SavedRequest savedRequest =  (org.springframework.security.web.savedrequest.SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST_KEY");

	if(savedRequest != null) {
		String requestedUrl = savedRequest.getRedirectUrl();

		if(requestedUrl != null) {
			session.setAttribute("requestedUrl", requestedUrl);

			if(requestedUrl.startsWith("/ir/info") || requestedUrl.startsWith("/ir/data")) {

				out.println("<em>"+requestedUrl+"</em> is protected content.  To access, you need to have been given a userid and password.");

				out.println("<br><a href=\"http://www.lib.unc.edu/forms/cdr/?subject=Request access to "+requestedUrl+"\">Request Access</a><br>");
			}
		}
	}
%>

<form method="POST" action="<c:url value="/j_spring_security_check"/>">
<table>
<tr><td>User:</td><td><input type='text' name='j_username'></td></tr>
<tr><td>Password:</td><td><input type='password' name='j_password'></td></tr>

<tr><td colspan='2'><input type="submit" value="Login"></td></tr>
<tr><td colspan='2'><input type="reset" value="Reset"></td></tr>
</table>
</form>
<%@ include file="html/footer.html"%>