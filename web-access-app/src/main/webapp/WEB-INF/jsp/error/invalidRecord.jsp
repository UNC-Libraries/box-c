<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="onecol">
	<div class="contentarea">
		<p>The record you attempted to access either does not exist or you do not have sufficient rights to view it.</p>  
		
		<p>If you have reached this page in error, please <a href="">report</a> it to us or return to the previous page in your browser.</p>

		<p>Or if you believe the record exists and would like to get access to it,  
			<c:if test="${empty sessionScope.user || empty sessionScope.user.userName}">
				try <a href="<c:out value='${loginUrl}'/>">logging in (UNC Onyen)</a> or
			</c:if>
			you may <a href="${contactUrl}">Contact Wilson Library for access information</a>.
		</p>
	</div>
</div>