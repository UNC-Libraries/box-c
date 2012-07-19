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
<script src="/static/js/fullRecord.js"></script>
<script src="/static/js/browseResults.js"></script>
<div class="onecol">
	<div class="contentarea">
		<c:set var="thumbUrl">
			<c:choose>
				<c:when test="${cdr:contains(briefObject.datastream, 'IMAGE_JP2000')}">
				</c:when>
				<c:when test="${cdr:contains(briefObject.datastream, 'DATA_FILE')}">
					<c:choose>
						<c:when test="${briefObject.contentType.searchKey == 'pdf'}">
							${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}
						</c:when>
						<c:when test="${briefObject.contentType.highestTierDisplayValue == 'mp4'}">
						</c:when>
						<c:when test="${briefObject.contentType.highestTierDisplayValue == 'mp3'}">
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
				<c:when test="${cdr:contains(briefObject.datastream, 'THUMB_LARGE')}">
					<div class="largethumb_container">
						<img id="thumb_main" class="largethumb ph_large_${briefObject.contentType.searchKey}" 
								src="${cdr:getDatastreamUrl(briefObject, 'THUMB_LARGE', fedoraUtil)}"/>
					</div>
				</c:when>
				<c:when test="${not empty briefObject.contentType.searchKey}">
					<div class="largethumb_container">
						<img id="thumb_main" class="largethumb ph_large_default" src="/static/images/placeholder/large/${briefObject.contentType.searchKey}.png"/>
					</div>
				</c:when>
				<c:otherwise>
					<div class="largethumb_container">
						<img id="thumb_main" class="largethumb ph_large_default" src="/static/images/placeholder/large/default.png"/>
					</div>
				</c:otherwise>
			</c:choose>
		</a>
		<div class="collinfo">
			<h2><c:out value="${briefObject.title}" /></h2>
			<c:if test="${not empty briefObject.creator}">
				<p class="smaller"><span class="bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span> 
					<c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
						<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
					</c:forEach>
				</p>
			</c:if>
			<c:choose>
				<c:when test="${cdr:contains(briefObject.datastream, 'DATA_FILE')}">
					<div class="actionlink left download">
						<a href="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}&dl=true">Download</a>
					</div>
				</c:when>
			</c:choose>
			
			<c:choose>
				<c:when test="${cdr:contains(briefObject.datastream, 'IMAGE_JP2000')}">
					<div class="actionlink left">
						<a href="" class="inline_viewer_link jp2_viewer_link">View</a>
					</div>
					<div class="clear_space"></div>
					<script src="/static/plugins/OpenLayers/OpenLayers.js"></script>
					
					<script type="text/javascript">
						$(function() {
							$(".inline_viewer_link").bind("click", {id: '${briefObject.id}', viewerId:'jp2_imageviewer_window',
								viewerContext: "${pageContext.request.contextPath}"}, displayJP2Viewer);
							if (window.location.hash.replace("#", "") == "showJP2"){
								$(".inline_viewer_link").trigger("click");
							}
						});
					  </script>
					  <div id="jp2_imageviewer_window" class="djatokalayers_window not_loaded">&nbsp;</div>
					
				</c:when>
				<c:when test="${cdr:contains(briefObject.datastream, 'DATA_FILE')}">
					<c:choose>
						<c:when test="${briefObject.contentType.searchKey == 'pdf'}">
							<div class="actionlink left">
								<a href="${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}">View</a>
							</div>
						</c:when>
						<c:when test="${briefObject.contentType.highestTierDisplayValue == 'mp3'}">
							<script src="/static/plugins/flowplayer/flowplayer-3.2.6.min.js"></script>
							<div class="actionlink left">
								<a href="" class="inline_viewer_link audio_player_link">Listen</a>
							</div>
							<div class="clear_space"></div>
							<div id="audio_player"></div>
							<c:set var="dataFileUrl">${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}&ext=.${briefObject.contentType.searchKey}</c:set>
							<script language="JavaScript">
								$(function() {
									$(".inline_viewer_link").bind("click", {viewerId:'audio_player',
										url: "${dataFileUrl}"}, displayAudioPlayer);
									if (window.location.hash.replace("#", "") == "showAudio"){
										$(".inline_viewer_link").trigger("click");
									}
								});
							</script>
						</c:when>
						<c:when test="${briefObject.contentType.highestTierDisplayValue == 'mp4'}">
							<div class="actionlink left">
								<a href="" class="inline_viewer_link video_viewer_link">View</a>
							</div>
							<div class="clear_space"></div>
							<script src="/static/plugins/flowplayer/flowplayer-3.2.6.min.js"></script>
							<div id="video_player"></div>
							<c:set var="dataFileUrl">${cdr:getDatastreamUrl(briefObject, 'DATA_FILE', fedoraUtil)}&ext=.${briefObject.contentType.searchKey}</c:set>
							<script language="JavaScript">
								$(function() {
									$(".inline_viewer_link").bind("click", {viewerId:'video_player',
										url: "${dataFileUrl}"}, displayVideoViewer);
									if (window.location.hash.replace("#", "") == "showVideo"){
										$(".inline_viewer_link").trigger("click");
									}
								});
							</script>
							<div class="clear"></div>
						</c:when>
					</c:choose>
				</c:when>
			</c:choose>
			<div class="clear"></div>
			<p class="smaller">
				<c:choose>
					<c:when test="${not empty briefObject.contentType.highestTierDisplayValue}">
						<span class="bold">File Type:</span> <c:out value="${briefObject.contentType.highestTierDisplayValue}" />
						<c:if test="${briefObject.filesize != -1}">  | <span class="bold">${searchSettings.searchFieldLabels[searchFieldKeys.FILESIZE]}:</span> <c:out value="${cdr:formatFilesize(briefObject.filesize, 1)}"/></c:if>
					</c:when>
					<c:otherwise>
						<span>Contains:</span> ${briefObject.childCount} item<c:if test="${briefObject.childCount != 1}">s</c:if>
					</c:otherwise>
				</c:choose>
				<c:if test="${not empty briefObject.dateAdded}">  | <span class="bold">${searchSettings.searchFieldLabels[searchFieldKeys.DATE_ADDED]}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></c:if>
				<c:if test="${not empty briefObject.dateCreated}">  | <span class="bold">${searchSettings.searchFieldLabels[searchFieldKeys.DATE_CREATED]}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></c:if>
			</p>
			
		</div>
	</div>
</div>
<c:if test="${hierarchicalViewResults.resultCount > 0}">
	<c:set var="defaultWebObjectID">
		<c:forEach items="${briefObject.datastream}" var="datastream">
			<c:if test="${datastream.name == 'DATA_FILE'}">
				<c:out value="${fn:substring(datastream.pid, 0, fn:indexOf(datastream.pid, '/'))}"/>
			</c:if>
		</c:forEach>
	</c:set>

	<div id="hierarchical_view_full_record" class="aggregate">
		<c:import url="WEB-INF/jsp/browseResults/hierarchicalBrowse.jsp">
			<c:param name="queryPath" value="search"/>
			<c:param name="applyCutoffs" value="true"/>
			<c:param name="displayCounts" value="true"/>
			<c:param name="hideTypeIcon">false</c:param>
			<c:param name="displaySecondaryActions" value="true"/>
			<c:param name="disableSecondarySearchPathLink" value="true"/>
			<c:param name="disableSecondaryBrowseLink" value="true"/>
			<c:param name="disableSecondarySearchWithStateLink" value="true"/>
			<c:param name="excludeParent" value="true"/>
			<c:param name="filePrimaryDownload" value="false"/>
			<c:param name="excludeIDs" value="${defaultWebObjectID}"/>
			<c:param name="showSeeAllLinks" value="false"/>
		</c:import>
	</div>
</c:if>

<div class="onecol shadowtop">
	<div class="contentarea">
		<c:if test="${briefObject['abstractText'] != null}">
			<div class="description">
				<p>
					<c:out value="${briefObject['abstractText']}" />
				</p>
			</div>
		</c:if>
		<div class="metadata">
			<table>
				<tr>
					<th>Contains:</th>
					<td>
						<c:url var="contentsResultsUrl" scope="page" value='search'>
							<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${briefObject.path.searchValue},${briefObject.path.highestTier + 1}"/>
						</c:url>
						<a href="<c:out value='${contentsResultsUrl}' />">${briefObject.childCount} item<c:if test="${briefObject.childCount != 1}">s</c:if></a>
					</td>
				</tr>
				<c:if test="${not empty facetFields}">
					<c:forEach items="${facetFields}" var="facetField">
						<tr>
							<th><c:out value="${searchSettings.searchFieldLabels[facetField.name]}" />:</th>
							<td>
								<ul>
									<c:forEach items="${facetField.values}" var="facetValue" varStatus="status">
										<li>
											<c:url var="facetActionUrl" scope="page" value='search'>
												<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" value="${searchSettings.searchFieldParams[searchFieldKeys.ANCESTOR_PATH]}:${briefObject.path.searchValue},${briefObject.path.highestTier + 1}"/>
												<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${additionalLimitActions}${searchSettings.actions["SET_FACET"]}:${searchSettings.searchFieldParams[facetValue.fieldName]},"${facetValue.searchValue}"'/>
											</c:url>
											<a href="<c:out value="${facetActionUrl}"/>"><c:out value="${facetValue.displayValue}" /></a> (<c:out value="${facetValue.count}" />)
										</li>
									</c:forEach>
								</ul>
							</td>
						</tr>
					</c:forEach>
				</c:if>
			</table>
		</div>
		<c:import url="WEB-INF/jsp/fullRecord/metadataBody.jsp" />
		<c:import url="WEB-INF/jsp/fullRecord/exports.jsp" />
	</div>
</div>
<c:import url="WEB-INF/jsp/fullRecord/neighborList.jsp" />