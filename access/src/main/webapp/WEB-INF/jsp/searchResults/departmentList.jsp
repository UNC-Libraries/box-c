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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<ul class="lightest twocol shadowtop browse_departments">
	<c:set var="halfwayPoint" value="${fn:length(departmentFacets.values) / 2}"/>
	<c:set var="halfwayPoint" value="${halfwayPoint + (1-(halfwayPoint % 1)) % 1 }"/>
	<c:forEach items="${departmentFacets.values}" var="departmentFacet" varStatus="status">
		<c:if test="${not empty departmentFacet.displayValue}">
			<c:if test="${status.count == halfwayPoint + 1}">
				<c:out value="</ul><ul class='light twocol light browse_departments'>" escapeXml="false" />
			</c:if>
			<li>
				<c:url var="resultsUrl" scope="page" value="search/${container.id}">
					<c:param name="dept" value='${departmentFacet.searchValue}'/>
				</c:url>
				<a href="<c:out value='${resultsUrl}' />"><c:out value="${departmentFacet.displayValue}"/></a> (<c:out value="${departmentFacet.count}"/>)
			</li>
		</c:if>
	</c:forEach>
</ul>