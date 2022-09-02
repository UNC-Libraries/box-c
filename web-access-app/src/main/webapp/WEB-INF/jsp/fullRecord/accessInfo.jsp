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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>

<c:if test="${not permsHelper.allowsPublicAccess(briefObject) && empty cdr:getUsername()}">
    <div class="restricted-access">
        <h2>This ${fn:toLowerCase(briefObject.resourceType)} has restricted content</h2>
        <c:if test="${permsHelper.enhancedAuthenticatedAccess(accessGroupSet, briefObject)}">
            <div class="actionlink"><a class="button" href="${loginUrl}"><i class="fa fa-id-card"></i> Log in for access (UNC Onyen)</a></div>
        </c:if>
        <div class="actionlink"><a class="button" href="https://library.unc.edu/wilson/contact/?refer=https%3A%2F%2Fdcr.lib.unc.edu"><i class="fa fa-envelope"></i> Contact Wilson Library for access</a></div>
    </div>
</c:if>
