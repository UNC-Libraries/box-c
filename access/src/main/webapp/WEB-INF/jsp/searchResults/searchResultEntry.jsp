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
<jsp:useBean id="accessGroupConstants" class="edu.unc.lib.dl.security.access.AccessGroupConstants" scope="request"/>

<c:choose>
	<c:when test="${param.resultNumber % 2 == 0 }">
		<c:set var="resultEntryClass" value="even" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="resultEntryClass" value="" scope="page"/>
	</c:otherwise>
</c:choose>
<div id="entry${metadata.id}" class="searchitem ${resultEntryClass}">
	<div class="contentarea">
		<%-- Link to full record of the current item --%>
		<c:url var="fullRecordUrl" scope="page" value="record">
			<c:param name="${searchSettings.searchStateParams['ID']}" value="${metadata.id}"/>
		</c:url>
		<c:url var="containerResultsUrl" scope="page" value='search'>
			<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${metadata.path.searchValue},${metadata.path.highestTier + 1}"/>
		</c:url>
		<%-- Set primary action URL based on content model and container results URL as appropriate --%>
		<c:choose>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
				<c:set var="primaryActionUrl" scope="page" value="${containerResultsUrl}"/>
				<c:set var="primaryActionTooltip" scope="page" value="View the contents of this folder."/>
			</c:when>
			<c:otherwise>
				<c:set var="primaryActionUrl" scope="page" value="${fullRecordUrl}"/>
				<c:set var="primaryActionTooltip" scope="page" value="View details for ${metadata.title}."/>
			</c:otherwise>
		</c:choose>
		
		<%-- Display thumbnail or placeholder graphic --%>
		<a href="<c:out value='${primaryActionUrl}' />" title="${primaryActionTooltip}" class="has_tooltip">
			<c:choose>
				<c:when test="${cdr:contains(metadata.datastream, 'THUMB_SMALL')}">
					<div class="smallthumb_container">
						<img id="thumb_${param.resultNumber}" class="smallthumb ph_small_${metadata.contentType.searchKey}" 
								src="${cdr:getDatastreamUrl(metadata.id, 'THUMB_SMALL', fedoraUtil)}"/>
					</div>
				</c:when>
				<c:otherwise>
					<c:choose>
						<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
							<div class="smallthumb_container">
								<img class="smallthumb" src="/static/images/placeholder/small/folder.png"/>
							</div>
						</c:when>
						<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection}">
							<div class="smallthumb_container">
								<img id="thumb_${param.resultNumber}" class="smallthumb ph_small_clear" 
										src="/static/images/collections/${metadata.idWithoutPrefix}.jpg" style="height: 64px; width: 64px;"/>
							</div>
						</c:when>
						<c:otherwise>
							<div class="smallthumb_container">
								<img id="thumb_${param.resultNumber}" class="smallthumb ph_small_default" 
										src="/static/images/placeholder/small/${metadata.contentType.searchKey}.png"/>
							</div>
						</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
		</a>
		<%-- Main result entry metadata body --%>
		<div class="iteminfo">
			<c:choose>
				<%-- Metadata body for containers --%>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection || metadata.resourceType == searchSettings.resourceTypeFolder}">
					<h2><a href="<c:out value='${primaryActionUrl}' />" title="${primaryActionTooltip}" class="has_tooltip"><c:out value="${metadata.title}"/></a>
						<c:if test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
							<p class="searchitem_container_count">(${metadata.childCount} item<c:if test="${metadata.childCount != 1}">s</c:if>)</p>
						</c:if>
					</h2>
					<div class="halfwidth">
						<c:choose>
							<c:when test="${not empty metadata.creator}">
								<p>${searchSettings.searchFieldLabels[searchFieldKeys.CREATOR]}:
									<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
										<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
									</c:forEach>
								</p>		
							</c:when>
							<c:otherwise>
								<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_ADDED]}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateAdded}" /></p>
							</c:otherwise>
						</c:choose>
					</div>
					<div class="halfwidth">
						<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_UPDATED]}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateUpdated}" /></p> 
					</div>
					<c:if test="${not empty metadata.abstractText}">
						<div class="clear"></div>
						<p>${searchSettings.searchFieldLabels[searchFieldKeys.ABSTRACT]}: 
							<c:out value="${cdr:truncateText(metadata.abstractText, 250)}"/>
							<c:if test="${fn:length(metadata.abstractText) > 250}">...</c:if>
							</p>
					</c:if>
				</c:when>
				<%-- Metadata body for items --%>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile}">
					<h2><a href="<c:out value='${primaryActionUrl}' />"><c:out value="${metadata.title}"/></a></h2>
					<div class="halfwidth">
						<c:if test="${not empty metadata.creator}">
							<p>${searchSettings.searchFieldLabels[searchFieldKeys.CREATOR]}: 
								<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
									<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
								</c:forEach>
							</p>
						</c:if>
						<p>
							<c:url var="parentUrl" scope="page" value="record">
								<c:param name="${searchSettings.searchStateParams['ID']}" value="${metadata.parentCollection}"/>
							</c:url>
							${searchSettings.searchFieldLabels[searchFieldKeys.PARENT_COLLECTION]}: <a href="<c:out value='${parentUrl}' />"><c:out value="${metadata.parentCollectionName}"/></a>
						</p>
					</div>
					<div class="halfwidth">
						<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_ADDED]}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateAdded}" /></p>
						<c:if test="${not empty metadata.dateCreated}">
							<c:set var="dateCreatedMonthDay" scope="page"><fmt:formatDate pattern="MM-dd" timeZone="GMT" value="${metadata.dateCreated}" /></c:set>
							<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_CREATED]}: 
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
					</div>
				</c:when>
			</c:choose>
		</div>
		
		<%-- Action buttons --%>
		<c:choose>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
				<div class="containerinfo">
					<c:url var="browseUrl" scope="page" value='browse?${searchStateUrl}'>
						<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${metadata.path.searchValue}"/>
						<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${searchSettings.actions['RESET_NAVIGATION']}:structure"/>
					</c:url>
					<ul>
						<li><a href="<c:out value='${fullRecordUrl}'/>" title="View folder information for ${metadata.title}" class="has_tooltip">View ${fn:toLowerCase(metadata.resourceType)} details</a></li>
						<li><a href="<c:out value='${browseUrl}'/>" title="View the structure of this folder in a file browser view." class="has_tooltip">Browse structure</a></li>
					</ul>
				</div>
			</c:when>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection}">
				<div class="containerinfo">
					<c:url var="browseUrl" scope="page" value='browse?${searchStateUrl}'>
						<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${metadata.path.searchValue}"/>
						<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${searchSettings.actions['RESET_NAVIGATION']}:structure"/>
					</c:url>
					<ul>
						<li><a href="<c:out value='${containerResultsUrl}'/>" title="View the contents of this collection" class="has_tooltip">View ${metadata.childCount} items</a></li>
						<li><a href="<c:out value='${browseUrl}'/>" title="View the structure of this collection in a file browser view." class="has_tooltip">Browse structure</a></li>
						<li>${metadata.resourceType}</li>
					</ul>
				</div>
			</c:when>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile}">
				<div class="fileinfo">
					<c:choose>
						<c:when test="${cdr:contains(metadata.datastream, 'DATA_FILE')}">
							<div class="actionlink right download">
								<a href="${cdr:getDatastreamUrl(metadata.id, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
							</div>
						</c:when>
						<c:when test="${cdr:contains(metadata.datastream, 'SURROGATE')}">
							<div class="actionlink right download">
								<a href="${cdr:getDatastreamUrl(metadata.id, 'SURROGATE', fedoraUtil)}">Preview</a>
							</div>
						</c:when>
						<c:otherwise>
							<div class="actionlink right login">
								<a href="${loginUrl}">Login</a>
							</div>
						</c:otherwise>
					</c:choose>
					
					<p class="right">
						<c:out value="${metadata.contentType.highestTierDisplayValue}"/>
						<c:if test="${not empty metadata.filesize}">
							&nbsp;(<c:out value="${cdr:formatFilesize(metadata.filesize, 1)}"/>)
						</c:if>
					</p>
					<p class="right">
						<c:choose>
							<c:when test="${cdr:contains(metadata.recordAccess, accessGroupConstants.PUBLIC_GROUP)}">
								<c:if test="${!cdr:contains(metadata.surrogateAccess, accessGroupConstants.PUBLIC_GROUP) 
											|| !cdr:contains(metadata.fileAccess, accessGroupConstants.PUBLIC_GROUP)}">
									Limited Access
								</c:if>
							</c:when>
							<c:otherwise>
								Restricted Access
							</c:otherwise>
						</c:choose>
					</p>
				</div>
			</c:when>
		</c:choose>
	</div>
</div>