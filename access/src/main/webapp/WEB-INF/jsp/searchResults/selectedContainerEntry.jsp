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
		<c:choose>
			<c:when test="${cdr:contains(metadata.datastream, 'THUMB_SMALL')}">
				<div class="smallthumb_container">
					<img class="smallthumb" src="${cdr:getDatastreamUrl(metadata, 'THUMB_SMALL', fedoraUtil)}"/>
				</div>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
						<div class="smallthumb_container">
							<img class="smallthumb" src="/static/images/placeholder/small/folder.png"/>
						</div>
					</c:when>
					<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate && not empty metadata.contentTypeFacet[0].searchKey}">
						<div class="smallthumb_container">
							<img class="smallthumb" src="/static/images/placeholder/small/${metadata.contentTypeFacet[0].searchKey}.png"/>
						</div>
					</c:when>
					<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate}">
						<div class="smallthumb_container">
							<img class="smallthumb" src="/static/images/placeholder/small/default.png"/>
						</div>
					</c:when>
					<c:otherwise>
						<div class="smallthumb_container">
							<img class="smallthumb" src="/static/images/collections/${metadata.idWithoutPrefix}.jpg"/>
						</div>
					</c:otherwise>
				</c:choose>
			</c:otherwise>
		</c:choose>
		<c:url var="fullRecordUrl" scope="page" value="record">
			<c:param name="${searchSettings.searchStateParams[searchFieldKeys.ID]}" value="${metadata.id}"/>
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
				<p>${searchSettings.searchFieldLabels[searchFieldKeys.CREATOR]}: 
					<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
						<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
					</c:forEach>
				</p>
			</c:if>
			<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_UPDATED]}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateUpdated}"/></p>
			
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
			<c:url var="browseUrl" scope="page" value='browse?${searchStateUrl}'>
				<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${metadata.path.searchValue}"/>
				<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value="${searchSettings.actions['RESET_NAVIGATION']}:structure"/>
			</c:url>
			
			<c:choose>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection}">
					<ul>
						<li><a href="<c:out value='${browseUrl}'/>">Browse structure</a></li>
					</ul>
				</c:when>
				<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate}">
					<c:choose>
						<c:when test="${cdr:contains(metadata.datastream, 'DATA_FILE')}">
							<div class="actionlink right download">
								<a href="${cdr:getDatastreamUrl(metadata, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
							</div>
						</c:when>
						<c:when test="${cdr:contains(metadata.datastream, 'SURROGATE')}">
							<div class="actionlink right download">
								<a href="${cdr:getDatastreamUrl(metadata, 'SURROGATE', fedoraUtil)}">Preview</a>
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
						<li><a href="<c:out value='${browseUrl}'/>">Browse structure</a></li>
					</ul>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>