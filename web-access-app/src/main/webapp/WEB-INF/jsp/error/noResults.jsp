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

<p class="no-search-results">Your search request returned no results.</p>

<p class="no-search-results">You may alter or broaden your search query using the navigation options on the left.  Alternatively,
	<c:if test="${empty sessionScope.user || empty sessionScope.user.userName}">
		try <a href="<c:out value='${loginUrl}'/>">logging in</a> or
	</c:if>
	you may <a href="">request</a> permission if there are results you are expecting which are not being displayed.
</p>