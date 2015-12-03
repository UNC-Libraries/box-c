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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:if test="${cdr:contains(metadata.status, 'Deleted') || cdr:contains(metadata.status, 'Parent Deleted')}">
	<c:set var="isDeleted" value="deleted" scope="page"/>
</c:if>
<c:if test="${not empty metadata && (not cdr:hasPatronRoleForPublicGroup(metadata) || not empty metadata.activeEmbargo)}">
	<c:set var="isProtected" value="protected" scope="page"/>
</c:if>

<c:choose>
	<c:when test="${not empty metadata.countMap}">
		<c:set var="childCount" value="${metadata.countMap.child}"/>
	</c:when>
	<c:otherwise>
		<c:set var="childCount" value="0"/>
	</c:otherwise>
</c:choose>

<c:set var="hasListAccessOnly" value="${cdr:hasListAccessOnly(requestScope.accessGroupSet, metadata)}"/>

<div id="entry${metadata.id}" class="browseitem ${isDeleted}${' '}${isProtected}">
	<div class="contentarea">
		<%-- Link to full record of the current item --%>
		<c:url var="fullRecordUrl" scope="page" value="record/${metadata.id}">
		</c:url>
		<%-- Set primary action URL based on content model and container results URL as appropriate --%>
		<c:url var="containerResultsUrl" scope="page" value='list/${metadata.id}'></c:url>
		<c:set var="primaryActionUrl" scope="page" value="${fullRecordUrl}"/>
		
		<c:set var="thumbnailObject" value="${metadata}" scope="request" />
		<c:import url="common/thumbnail.jsp">
			<c:param name="target" value="record" />
			<c:param name="size" value="large" />
		</c:import>

		<h2>
			<a href="<c:out value='${primaryActionUrl}' />" class="has_tooltip" title="View details for ${metadata.title}."><c:out value="${metadata.title}"/></a>
			<c:choose>
				<c:when test="${hasListAccessOnly}">
					<span class="searchitem_container_count">(<c:if test="${not empty loginUrl}"><a href="${loginUrl}">log in</a> or </c:if><a href="${contactUrl}&requestpid=${metadata.pid.pid}">request access</a>)</span>
				</c:when>
				<c:otherwise> 
					<span class="searchitem_container_count">(${childCount} item<c:if test="${childCount != 1}">s</c:if>)</span>
				</c:otherwise>
			</c:choose>
		</h2>
		<c:if test="${not empty metadata.creator}">
			<p>${searchSettings.searchFieldLabels['CREATOR']}: 
				<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
					<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
				</c:forEach>
			</p>
		</c:if>
		<p>${searchSettings.searchFieldLabels['DATE_UPDATED']}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateUpdated}"/></p>
		<p><c:out value="${metadata['abstractText']}"/></p>
	</div>
</div>