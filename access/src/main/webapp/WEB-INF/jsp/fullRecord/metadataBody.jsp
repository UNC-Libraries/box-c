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
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<div class="metadata">
	<table>
		<c:set var="searchStateUrl" scope="request" value=""/>
		<c:set var="objectPath" scope="request" value="${briefObject.objectPath}"/>
		<tr>
			<th>${searchSettings.searchFieldLabels['ANCESTOR_PATH']}</th>
			<td>
				<c:import url="/jsp/util/pathTrail.jsp">
					<c:param name="hideLast"><c:choose><c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">true</c:when><c:otherwise>false</c:otherwise></c:choose></c:param>
					<c:param name="linkLast">true</c:param>
					<c:param name="queryPath">list</c:param>
				</c:import>
			</td>
		</tr>
		<%-- Display parent collection if it is not empty and this is not a root node --%>
		<c:if test="${not empty briefObject.parentCollection && briefObject.ancestorPathFacet.highestTier > 0}">
			<tr>
				<th>Parent Collection</th>
				<td>
					<c:url var="parentUrl" scope="page" value="record/${briefObject.parentCollection}">
					</c:url>
					<a href="<c:out value='${parentUrl}' />"><c:out value="${briefObject.parentCollectionName}"/></a>
				</td>
			</tr>
		</c:if>
	</table>
</div>
<c:if test="${not empty fullObjectView}">
	<div class="metadata">
		${fullObjectView}
	</div>
</c:if>

