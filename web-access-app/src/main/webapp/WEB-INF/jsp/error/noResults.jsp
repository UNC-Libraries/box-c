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