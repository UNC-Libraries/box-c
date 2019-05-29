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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div id="entry${metadata.id}" class="searchitem selected_container even">
	<div class="selected_container_header">
		<p>
			<c:choose>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate}">
					Containing item
				</c:when>
				<c:otherwise>
					Current ${metadata.resourceType}
				</c:otherwise>
			</c:choose>
		</p>
	</div>
	<div class="contentarea">
		<c:set var="thumbnailObject" value="${metadata}" scope="request" />
		<c:import url="common/thumbnail.jsp">
			<c:param name="target" value="record" />
			<c:param name="size" value="small" />
		</c:import>
		
		<c:url var="fullRecordUrl" scope="page" value="record/${metadata.id}">
		</c:url>
		<div class="iteminfo">
			<h2>
				<c:choose>
					<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection || metadata.resourceType == searchSettings.resourceTypeAggregate}">
						<a href="<c:out value='${fullRecordUrl}'/>"><c:out value="${metadata.title}"/></a>
					</c:when>
					<c:otherwise>
						<c:out value="${metadata.title}"/>
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
			<c:set var="embargoDate" value="${metadata.activeEmbargo}"/>
			<c:if test="${not empty embargoDate}"><p>Embargoed Until: <fmt:formatDate pattern="yyyy-MM-dd" value="${embargoDate}" /></p></c:if>
			
			<c:if test="${not empty metadata['abstractText']}">
				<c:set var="truncatedAbstract" value="${cdr:truncateText(metadata.abstractText, 250)}"/>
				<p>
					<c:out value="${truncatedAbstract}" />
					<c:if test="${fn:length(metadata.abstractText) > 250}">
						(<a href="<c:out value='${fullRecordUrl}' />">more</a>)
					</c:if>
				</p>
			</c:if>
		</div>
		<div class="containerinfo">
			<c:choose>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection}">
					<ul>
						<li><a href="<c:out value='structure/${metadata.id}'/>">Browse structure</a></li>
					</ul>
				</c:when>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate}">
					<c:choose>
						<c:when test="${permsHelper.hasOriginalAccess(requestScope.accessGroupSet, metadata)}">
							<div class="actionlink right download">
								<a href="${cdr:getOriginalFileUrl(metadata)}?dl=true">Download</a>
							</div>
						</c:when>
						<c:otherwise>
							<ul>
								<li><a href="<c:out value='${fullRecordUrl}'/>">View details</a></li>
							</ul>
						</c:otherwise>
					</c:choose>
					<c:if test="${not empty metadata.contentTypeFacet}">
						<p class="right">
							<c:out value="${metadata.contentTypeFacet[0].displayValue}"/>
							<c:if test="${not empty metadata.filesizeSort}">
								&nbsp;(<c:out value="${cdr:formatFilesize(metadata.filesizeSort, 1)}"/>)
							</c:if>
						</p>
					</c:if>
				</c:when>
				<c:otherwise>
					<ul>
						<li><a href="<c:out value='${fullRecordUrl}'/>">View ${fn:toLowerCase(metadata.resourceType)} details</a></li>
						<li><a href="<c:out value='structure/${metadata.id}'/>">Browse structure</a></li>
					</ul>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>