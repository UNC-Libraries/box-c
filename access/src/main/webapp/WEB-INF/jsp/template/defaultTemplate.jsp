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
<?xml version="1.0"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<c:import url="WEB-INF/jsp/common/headElements.jsp" />
	<title>
		Carolina Digital Repository<c:if test="${not empty pageSubtitle}"> - <c:out value="${pageSubtitle}"/></c:if>
	</title>
</head>
<body>
<div id="pagewrap">
	<div id="pagewrap_inside">
		<c:import url="WEB-INF/jsp/common/header.jsp" />
		<div id="content">
			<c:choose>
				<c:when test="${not empty contentPage}">
					<c:import url="${contentPage}" />
				</c:when>
				<c:otherwise>
					<c:import url="WEB-INF/jsp/error/404.jsp" />
				</c:otherwise>
			</c:choose>
		</div>
		<c:import url="WEB-INF/jsp/common/footer.jsp"/>
	</div>
</div>
</body>
</html>