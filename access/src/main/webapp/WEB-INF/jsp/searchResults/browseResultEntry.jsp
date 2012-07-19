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
<c:choose>
	<c:when test="${param.resultNumber % 2 == 0 }">
		<c:set var="resultEntryClass" value="even" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="resultEntryClass" value="" scope="page"/>
	</c:otherwise>
</c:choose>
<div id="entry${metadata.id}" class="browseitem ${resultEntryClass}">
	<div class="contentarea">
		<%-- Link to full record of the current item --%>
		<c:url var="fullRecordUrl" scope="page" value="record">
			<c:param name="${searchSettings.searchStateParams['ID']}" value="${metadata.id}"/>
		</c:url>
		<%-- Set primary action URL based on content model and container results URL as appropriate --%>
		<c:url var="containerResultsUrl" scope="page" value='search'>
			<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${metadata.path.searchValue},${metadata.path.highestTier + 1}"/>
		</c:url>
		<c:set var="primaryActionUrl" scope="page" value="${fullRecordUrl}"/>
		
		<%-- Display thumbnail or placeholder --%>
		<a href="<c:out value='${primaryActionUrl}' />">
			<c:choose>
				<c:when test="${cdr:contains(metadata.datastream, 'THUMB_LARGE')}">
					<div class="largethumb_container">
						<%--<img id="thumb_${param.resultNumber}" class="largethumb ph_large_${metadata.resourcetype}" 
								src="${cdr:getDatastreamUrl(metadata, 'THUMB_LARGE', fedoraUtil)}"/> --%>
						<img class="largethumb" src="${cdr:getDatastreamUrl(metadata, 'THUMB_LARGE', fedoraUtil)}"/>
					</div>
				</c:when>
				<c:otherwise>
					<div class="largethumb_container">
						<img id="thumb_${param.resultNumber}" class="largethumb" 
								src="/static/images/collections/${metadata.idWithoutPrefix}.jpg"/>
					</div>
				</c:otherwise>
			</c:choose>
		</a>
		
		<h2>
			<a href="<c:out value='${primaryActionUrl}' />" class="has_tooltip" title="View details for ${metadata.title}."><c:out value="${metadata.title}"/></a> 
			<p class="searchitem_container_count">(${metadata.childCount} item<c:if test="${metadata.childCount != 1}">s</c:if>)</p>
		</h2>
		<c:if test="${not empty metadata.creator}">
			<p>${searchSettings.searchFieldLabels[searchFieldKeys.CREATOR]}: 
				<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
					<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
				</c:forEach>
			</p>
		</c:if>
		<p>${searchSettings.searchFieldLabels[searchFieldKeys.DATE_UPDATED]}: <fmt:formatDate pattern="yyyy-MM-dd" value="${metadata.dateUpdated}"/></p>
		<p><c:out value="${metadata['abstractText']}"/></p>
	</div>
</div>