<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<c:if test="${sessionScope.accessLevel != null && sessionScope.accessLevel.viewAdmin}">
    <c:param name="accessLevel" value="admin"/>
</c:if>
<c:if test="${not empty cdr:getUsername()}">
    <c:param name="username" value="${cdr:getUsername()}"/>
</c:if>

<div id="app"></div>
<script type="module" crossorigin src="static/js/vue-access-index.js"></script>