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
<%@ page import="org.springframework.security.saml.metadata.MetadataManager" %>
<%@ page import="org.springframework.web.context.WebApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="java.util.Set" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<h1>IDP selection</h1>

<%
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletConfig().getServletContext());
    MetadataManager mm = context.getBean("metadata", MetadataManager.class);
    Set<String> idps = mm.getIDPEntityNames();
    pageContext.setAttribute("idp", idps);
%>

<p>
<form action="saml/login" method="GET">
    <%-- We send this attribute to tell the processing filter that we want to initialize login --%>
    <input type="hidden" name="login" value="true"/>
    <table>
        <tr>
            <td><b>Select IDP: </b></td>
            <td>
                <c:forEach var="idpItem" items="${idp}">
                    <input type="radio" name="idp" id="idp_<c:out value="${idpItem}"/>" value="<c:out value="${idpItem}"/>"/>
                    <label for="idp_<c:out value="${idpItem}"/>"><c:out value="${idpItem}"/></label>
                    <br/>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td><input type="submit" value="Login"/></td>
        </tr>
    </table>
</form>
</p>