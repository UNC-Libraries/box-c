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

<c:choose>
	<c:when test="${not empty briefObject.countMap}">
		<c:set var="childCount" value="${briefObject.countMap.child}"/>
	</c:when>
	<c:otherwise>
		<c:set var="childCount" value="0"/>
	</c:otherwise>
</c:choose>

<c:set var="dataFileUrl">${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}</c:set>

<div class="full_record_top border-box-left-top">
	<div class="contentarea">
		<div class="collinfo">
			<div class="collinfo_metadata">
				<div class="columns">
					<div class="column is-2">
						<c:set var="thumbnailObject" value="${briefObject}" scope="request" />
						<c:import url="common/thumbnail.jsp">
							<c:param name="target" value="file" />
							<c:param name="size" value="large" />
						</c:import>
					</div>
					<div class="column is-8">
						<h2><c:out value="${briefObject.title}" /></h2>
						<div class="column is-12">
							<ul class="pipe_list smaller">
								<c:if test="${not empty briefObject.creator}">
									<li>
										<span class="bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span>
										<c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
											<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
										</c:forEach>
									</li>
								</c:if>
								<c:if test="${not empty briefObject.parentCollection && briefObject.ancestorPathFacet.highestTier > 0}">
									<li>
										<c:url var="parentUrl" scope="page" value="record/${briefObject.parentCollection}" />
										<span class="bold">Collection:</span>
										<a href="<c:out value='${parentUrl}' />"><c:out value="${briefObject.parentCollectionName}"/></a>
									</li>
								</c:if>
							</ul>

							<ul class="pipe_list smaller">
								<li>
									<c:choose>
										<c:when test="${not empty briefObject.contentTypeFacet[0].displayValue}">
											<span class="bold">File Type:</span> <c:out value="${briefObject.contentTypeFacet[0].displayValue}" />
											<c:if test="${briefObject.filesizeSort != -1}">  | <span class="bold">${searchSettings.searchFieldLabels['FILESIZE']}:</span> <c:out value="${cdr:formatFilesize(briefObject.filesizeSort, 1)}"/></c:if>
										</c:when>
										<c:otherwise>
											<span>Contains:</span> ${childCount} item<c:if test="${childCount != 1}">s</c:if>
										</c:otherwise>
									</c:choose>
								</li>
								<c:if test="${not empty briefObject.dateAdded}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_ADDED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></li></c:if>
								<c:if test="${not empty briefObject.dateCreated}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_CREATED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></li></c:if>
								<c:if test="${not empty embargoDate}"><li><span class="bold">Embargoed Until:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${embargoDate}" /></li></c:if>
							</ul>
						</div>
					</div>
					<div class="column is-3 action-btn">
						<c:choose>
							<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', briefObject)}">
								<div class="actionlink right download">
									<a href="${dataFileUrl}?dl=true"><i class="fa fa-download"></i> Download</a>
								</div>
							</c:when>
							<c:when test="${not empty embargoDate && not empty dataFileUrl}">
								<div class="noaction right">
									Available after <fmt:formatDate value="${embargoDate}" pattern="d MMMM, yyyy"/>
								</div>
							</c:when>
						</c:choose>

						<c:if test="${cdr:hasAccess(accessGroupSet, briefObject, 'editDescription')}">
							<div class="actionlink right"><a href="${adminBaseUrl}/describe/${briefObject.id}">Edit</a></div>
						</c:if>
					</div>
				</div>
			</div>
		</div>
		<div class="clear">
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'IMAGE_JP2000', briefObject)}">
					<div class="clear_space"></div>
					<link rel="stylesheet" href="/static/plugins/leaflet/leaflet.css">
					<link rel="stylesheet" href="/static/plugins/Leaflet-fullscreen/dist/leaflet.fullscreen.css">
					<div id="jp2_viewer" class="jp2_imageviewer_window" data-url="${cdr:getPreferredDatastream(briefObject, 'IMAGE_JP2000').owner}"></div>
				</c:when>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DATA_FILE', briefObject)}">
					<c:choose>
						<c:when test="${briefObject.contentTypeFacet[0].displayValue == 'mp3'}">
							<div class="actionlink left">
								<a href="" class="inline_viewer_link audio_player_link">Listen</a>
							</div>
							<div class="clear_space"></div>
							<audio class="audio_player inline_viewer" src="${dataFileUrl}">
							</audio>
						</c:when>
					</c:choose>
				</c:when>
			</c:choose>
		</div>
	</div>
</div>

<c:if test="${childCount > 0}">
	<c:set var="defaultWebObjectID">
		<c:forEach items="${briefObject.datastreamObjects}" var="datastream">
			<c:if test="${datastream.name == 'DATA_FILE'}">
				<c:out value="${datastream.owner.pid}"/>
			</c:if>
		</c:forEach>
	</c:set>
	<link rel="stylesheet" href="/static/plugins/DataTables/datatables.min.css">

	<div class="child-records">
		<h3>List of Items in This Work (${childCount})</h3>
		<table id="child-files" data-pid="${briefObject.id}">
			<thead>
			<tr>
				<th>Thumbnail</th>
				<th>Title</th>
				<th>File Type</th>
				<th>File Size</th>
				<th></th>
				<th></th>
				<c:if test="${cdr:hasAccess(accessGroupSet, briefObject, 'editDescription')}">
					<th></th>
				</c:if>
			</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
	</div>
</c:if>
<c:import url="fullRecord/neighborList.jsp" />