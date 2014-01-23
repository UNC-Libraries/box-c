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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<div class="onecol full_record_top">
	<div class="contentarea">
		<c:set var="thumbUrl">
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'IMAGE_JP2000', briefObject)}">
				</c:when>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', briefObject)}">
					<c:choose>
						<c:when test="${briefObject.contentTypeFacet[0].searchKey == 'pdf'}">
							${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}
						</c:when>
						<c:when test="${briefObject.contentTypeFacet[0].displayValue == 'mp4'}">
						</c:when>
						<c:when test="${briefObject.contentTypeFacet[0].displayValue == 'mp3'}">
						</c:when>
						<c:otherwise>
							${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}
						</c:otherwise>
					</c:choose>
				</c:when>
			</c:choose>
		</c:set>
		
		<a href="${thumbUrl}" class="thumb_link">
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'THUMB_LARGE', briefObject)}">
					<div class="large thumb_container">
						<img id="thumb_main" class="largethumb ph_large_${briefObject.contentTypeFacet[0].searchKey}" 
								src="${cdr:getDatastreamUrl(briefObject, 'THUMB_LARGE', fedoraUtil)}"/>
					</div>
				</c:when>
				<c:otherwise>
					<div class="large thumb_container">
						<img id="thumb_main" class="largethumb ph_large_default" src="/static/images/placeholder/large/${briefObject.contentTypeFacet[0].searchKey}.png"/>
					</div>
				</c:otherwise>
			</c:choose>
		</a>
		<div class="collinfo">
			<div class="collinfo_metadata">
				<h2><c:out value="${briefObject.title}" /></h2>
				<c:if test="${not empty briefObject.creator}">
					<p class="smaller"><span class="bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span> 
						<c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
							<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
						</c:forEach>
					</p>
				</c:if>
				<p class="smaller">
					<span class="bold">File Type:</span> <c:out value="${briefObject.contentTypeFacet[0].displayValue}" />
					<c:if test="${briefObject.filesizeSort != -1}">  | <span class="bold">${searchSettings.searchFieldLabels['FILESIZE']}:</span> <c:out value="${cdr:formatFilesize(briefObject.filesizeSort, 1)}"/></c:if>
					<c:if test="${not empty briefObject.dateAdded}">  | <span class="bold">${searchSettings.searchFieldLabels['DATE_ADDED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></c:if>
					<c:if test="${not empty briefObject.dateCreated}">  | <span class="bold">${searchSettings.searchFieldLabels['DATE_CREATED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></c:if>
				</p>
			</div>
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', briefObject)}">
					<div class="actionlink left download">
						<a href="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}?dl=true">Download</a>
					</div>
				</c:when>
			</c:choose>
			
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'IMAGE_JP2000', briefObject)}">
					<div class="actionlink left">
						<a class="inline_viewer_link jp2_viewer_link">View</a>
					</div>
					<div class="clear_space"></div>
					<div id="jp2_viewer" class="jp2_imageviewer_window djatokalayers_window" data-url='${briefObject.id}'></div>
				</c:when>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', briefObject)}">
					<c:choose>
						<c:when test="${briefObject.contentTypeFacet[0].searchKey == 'pdf'}">
							<div class="actionlink left">
								<a href="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}">View</a>
							</div>
						</c:when>
						<c:when test="${briefObject.contentTypeFacet[0].displayValue == 'mp3'}">
							<div class="actionlink left">
								<a class="inline_viewer_link audio_player_link">Listen</a>
							</div>
							<div class="clear_space"></div>
							<audio class="audio_player inline_viewer" src="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}">
							</audio>
						</c:when>
						<c:when test="${briefObject.contentTypeFacet[0].displayValue == 'mp4'}">
							<div class="actionlink left">
								<a class="inline_viewer_link video_player_link">View</a>
							</div>
							<div class="clear_space"></div>
							<link rel="stylesheet" type="text/css" href="/static/plugins/flowplayer/skin/minimalist.css">
							<div class="video_player inline_viewer">
								<video>
									<source type="video/mp4" src="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}"></source>
								</video>
							</div>
							<div class="clear"></div>
						</c:when>
					</c:choose>
				</c:when>
			</c:choose>
		</div>
	</div>
</div>
<div class="onecol shadowtop">
	<div class="contentarea">
		<c:if test="${briefObject['abstractText'] != null}">
			<div class="description">
				<p>
					<c:out value="${briefObject['abstractText']}" />
				</p>
			</div>
		</c:if>
		<c:import url="fullRecord/metadataBody.jsp" />
		<c:import url="fullRecord/exports.jsp" />
	</div>
</div>
<c:import url="fullRecord/neighborList.jsp" />