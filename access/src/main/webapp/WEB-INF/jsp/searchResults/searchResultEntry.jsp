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

<c:if test="${cdr:contains(metadata.status, 'Deleted') || cdr:contains(metadata.status, 'Parent Deleted')}">
	<c:set var="isDeleted" value="deleted" scope="page"/>
</c:if>
<c:if test="${not empty metadata && (not permsHelper.allowsPublicAccess(metadata) || not empty metadata.activeEmbargo)}">
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
<c:set var="embargoDate" value="${metadata.activeEmbargo}"/>
<div id="entry${metadata.id}" class="searchitem ${isDeleted}${' '}${isProtected}">
	<div class="contentarea">
		<%-- Link to full record of the current item --%>
		<c:url var="fullRecordUrl" scope="page" value="record/${metadata.id}">
		</c:url>
		<c:url var="containerResultsUrl" scope="page" value='list/${metadata.id}'></c:url>
		
		<%-- Set primary action URL based on content model and container results URL as appropriate --%>
		<c:choose>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
				<c:set var="primaryActionUrl" scope="page" value="${containerResultsUrl}"/>
				<c:set var="primaryActionTooltip" scope="page" value="View the contents of this folder."/>
				<c:set var="thumbnailTarget" scope="page" value="list"/>
			</c:when>
			<c:otherwise>
				<c:set var="primaryActionUrl" scope="page" value="${fullRecordUrl}"/>
				<c:set var="primaryActionTooltip" scope="page" value="View details for ${metadata.title}."/>
				<c:set var="thumbnailTarget" scope="page" value="record"/>
			</c:otherwise>
		</c:choose>
		
		<%-- Display thumbnail or placeholder graphic --%>
		<c:set var="thumbnailObject" value="${metadata}" scope="request" />
		<c:import url="common/thumbnail.jsp">
			<c:param name="target" value="${thumbnailTarget}" />
			<c:param name="size" value="large" />
		</c:import>

		<%-- Main result entry metadata body --%>
		<div class="iteminfo is-size-6-desktop">
			<c:choose>
				<%-- Metadata body for containers --%>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection || metadata.resourceType == searchSettings.resourceTypeFolder}">
					<h2 class="iteminfo is-size-3-desktop">
						<a href="<c:out value='${primaryActionUrl}' />" title="${primaryActionTooltip}" class="has_tooltip"><c:out value="${metadata.title}"/></a>
						<c:if test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
							<span class="searchitem_container_count">(${childCount} item<c:if test="${childCount != 1}">s</c:if>)</span>
						</c:if>
					</h2>
					
					<div>
						<c:if test="${not empty metadata.creator}">
							<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['CREATOR']}</span>:
								<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
									<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
								</c:forEach>
							</p>
						</c:if>
						<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_ADDED']}</span>: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateAdded}" /></p>
						<c:if test="${not empty metadata.parentCollection && metadata.resourceType == searchSettings.resourceTypeFolder}">
							<p>
								<c:url var="parentUrl" scope="page" value="record/${metadata.parentCollection}" />
								${searchSettings.searchFieldLabels['PARENT_COLLECTION']}: <a href="<c:out value='${parentUrl}' />"><c:out value="${metadata.parentCollectionName}"/></a>
							</p>
						</c:if>
						<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_UPDATED']}</span>: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateUpdated}" /></p>
					</div>
					<c:if test="${not empty metadata.abstractText}">
						<div class="clear"></div>
						<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['ABSTRACT']}</span>:
							<c:out value="${cdr:truncateText(metadata.abstractText, 250)}"/>
							<c:if test="${fn:length(metadata.abstractText) > 250}">...</c:if>
							</p>
					</c:if>
				</c:when>
				<%-- Metadata body for items --%>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile || metadata.resourceType == searchSettings.resourceTypeAggregate}">
					<h2 class="iteminfo is-size-3-desktop">
						<a href="<c:out value='${primaryActionUrl}' />"><c:out value="${metadata.title}"/></a>
						<c:if test="${metadata.resourceType == searchSettings.resourceTypeAggregate && childCount > 1}">
							<span class="searchitem_container_count">(${childCount} item<c:if test="${childCount != 1}">s</c:if>)</span>
						</c:if>
					</h2>
					<div>
						<c:if test="${not empty metadata.creator}">
							<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['CREATOR']}</span>:
								<c:choose>
									<c:when test="${fn:length(metadata.creator) > 5}"><c:set var="creatorList" value="${cdr:subList(metadata.creator, 0, 5)}"/></c:when>
									<c:otherwise><c:set var="creatorList" value="${metadata.creator}"/></c:otherwise>
								</c:choose>
								
								<c:forEach var="creatorObject" items="${creatorList}" varStatus="creatorStatus">
									<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
								</c:forEach>
								
								<c:if test="${fn:length(metadata.creator) > 5}">; et al.</c:if>
							</p>
						</c:if>
						<c:if test="${not empty metadata.parentCollection}">
							<p>
								<c:url var="parentUrl" scope="page" value="record/${metadata.parentCollection}" />
								${searchSettings.searchFieldLabels['PARENT_COLLECTION']}: <a href="<c:out value='${parentUrl}' />"><c:out value="${metadata.parentCollectionName}"/></a>
							</p>
						</c:if>
						<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_ADDED']}</span>: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateAdded}" /></p>
						<c:if test="${not empty metadata.dateCreated}">
							<c:set var="dateCreatedMonthDay" scope="page"><fmt:formatDate pattern="MM-dd" timeZone="GMT" value="${metadata.dateCreated}" /></c:set>
							<p><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_CREATED']}</span>:
								<c:choose>
									<c:when test="${dateCreatedMonthDay == '01-01'}">
										<fmt:formatDate pattern="yyyy" timeZone="GMT" value="${metadata.dateCreated}" />
									</c:when>
									<c:otherwise>
										<fmt:formatDate pattern="yyyy-MM-dd" timeZone="GMT" value="${metadata.dateCreated}" />
									</c:otherwise>
								</c:choose>
							</p>
						</c:if>
						<c:if test="${not empty embargoDate}"><p><span class="has-text-weight-bold">Embargoed Until</span>: <fmt:formatDate pattern="yyyy-MM-dd" value="${embargoDate}" /></p></c:if>
					</div>
				</c:when>
			</c:choose>
		</div>
	</div>
</div>