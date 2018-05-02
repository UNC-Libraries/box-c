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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<c:set var="resourceType">
	<c:choose>
		<c:when test="${thumbnailObject.resourceType == searchSettings.resourceTypeFolder}">folder</c:when>
		<c:when test="${thumbnailObject.resourceType == searchSettings.resourceTypeCollection}">collection</c:when>
		<c:otherwise>document</c:otherwise>
	</c:choose>
</c:set>

<c:set var="contentType">
	<c:if test="${not empty thumbnailObject.contentTypeFacet[0].displayValue && thumbnailObject.contentTypeFacet[0].displayValue != 'unknown'}">
		<c:out value="${thumbnailObject.contentTypeFacet[0].displayValue}" />
	</c:if>
</c:set>

<c:set var="href">
	<c:choose>
		<c:when test="${param.target == 'file' && permsHelper.hasOriginalAccess(requestScope.accessGroupSet, thumbnailObject)}">
			<c:out value="${cdr:getOriginalFileUrl(thumbnailObject)}" />
		</c:when>
		<c:when test="${param.target == 'record'}">
			<c:out value="record/${thumbnailObject.id}" />
		</c:when>
		<c:when test="${param.target == 'list'}">
			<c:out value="list/${thumbnailObject.id}" />
		</c:when>
		<c:otherwise>
			<c:out value="record/${thumbnailObject.id}" />
		</c:otherwise>
	</c:choose>
</c:set>

<c:set var="tooltip">
  <c:choose>
    <c:when test="${param.target == 'file' && permsHelper.hasOriginalAccess(requestScope.accessGroupSet, thumbnailObject)}">
      View ${thumbnailObject.title}.
    </c:when>
    <c:when test="${param.target == 'record'}">
      View details for ${thumbnailObject.title}.
    </c:when>
    <c:when test="${param.target == 'list'}">
      View the contents of ${thumbnailObject.title}.
    </c:when>
  </c:choose>
</c:set>

<c:set var="src">
	<c:choose>
		<c:when test="${param.size == 'large' && permsHelper.hasThumbnailAccess(requestScope.accessGroupSet, thumbnailObject)}">
			<c:out value="${cdr:getDatastreamUrl(thumbnailObject, 'THUMB_LARGE')}" />
		</c:when>
		<c:when test="${param.size == 'small' && permsHelper.hasThumbnailAccess(requestScope.accessGroupSet, thumbnailObject)}">
			<c:out value="${cdr:getDatastreamUrl(thumbnailObject, 'THUMB_SMALL')}" />
		</c:when>
	</c:choose>
</c:set>

<c:set var="deleted" value="${cdr:contains(thumbnailObject.status, 'Deleted') || cdr:contains(thumbnailObject.status, 'Parent Deleted')}" />

<c:set var="badgeIcon">
	<c:choose>
		<c:when test="${deleted}">
			trash
		</c:when>
		<c:when test="${not empty thumbnailObject && (not permsHelper.allowsPublicAccess(thumbnailObject) || not empty thumbnailObject.activeEmbargo)}">
			lock
		</c:when>
	</c:choose>
</c:set>

<a href="${href}" title="<c:out value='${tooltip}' />" class="thumbnail ${empty src ? ' placeholder' : ''} ${deleted ? ' deleted' : ''} ${not empty tooltip ? ' has_tooltip' : ''} thumbnail-resource-type-${resourceType} thumbnail-size-${param.size}">
	<div class="thumbnail-placeholder">
		<c:if test="${not empty contentType}">
			<span class="thumbnail-content-type">${contentType}</span>
		</c:if>
	</div>
	<c:if test="${not empty src}">
		<div class="thumbnail-preview">
			<img src="${src}" />
		</div>
	</c:if>
	<c:if test="${not empty badgeIcon}">
		<div class="thumbnail-badge thumbnail-badge-${badgeIcon}">
			<div class="fa-stack">
				<i class="fa fa-circle fa-stack-2x background"></i>
				<i class="fa fa-${badgeIcon} fa-stack-1x foreground"></i>
			</div>
		</div>
	</c:if>
</a>
