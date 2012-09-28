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

<div class="onecol">
	<div class="contentarea">
		<p>The record you attempted to access either does not exist or you do not have sufficient rights to view it.</p>  
		
		<p>If you have reached this page in error, please <a href="">report</a> it to us or return to the previous page in your browser.</p>

		<p>Or if you believe the record exists and would like to get access to it,  
			<c:if test="${empty sessionScope.user || empty sessionScope.user.userName}">
				try <a href="<c:out value='${loginUrl}'/>">logging in</a> or
			</c:if>
			you may <a href="">request</a> permission.
		</p>
	</div>
</div>