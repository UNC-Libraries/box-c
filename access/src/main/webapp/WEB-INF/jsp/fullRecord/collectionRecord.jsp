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

<div class="onecol container_record full_record_top" id="full_record">
	<c:url var="structureUrl" scope="page" value='structure/${briefObject.id}'></c:url>

	<div class="contentarea">
		<div class="large thumb_container">
		<c:choose>
			<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'THUMB_LARGE', briefObject)}">
				<img id="thumb_main" class="largethumb" src="${cdr:getDatastreamUrl(briefObject, 'THUMB_LARGE', fedoraUtil)}"/>
			</c:when>
			<c:when test="${briefObject.resourceType == searchSettings.resourceTypeFolder}">
				<img id="thumb_main" class="largethumb" src="/static/images/placeholder/small/folder.png"/>
			</c:when>
			<c:otherwise>
				<img id="thumb_main" class="largethumb" src="/static/images/placeholder/large/collection.png"/>
			</c:otherwise>
		</c:choose>
		</div>
		
		<div class="collinfo">
			<h2><c:out value="${briefObject.title}" /></h2>
			<c:if test="${not empty briefObject.creator}">
				<p class="smaller"><span class="bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span> 
					<c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
						<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">, </c:if>
					</c:forEach>
				</p>
			</c:if>
			<c:if test="${briefObject['abstractText'] != null}">
					<p class="clear">
						<c:out value="${briefObject['abstractText']}" />
					</p>
			</c:if>
			<form id="collectionsearch" action="basicSearch" method="get">
				<div id="csearch_inputwrap">
					<input type="text" name="query" id="csearch_text" placeholder="Search the ${fn:toLowerCase(briefObject.resourceType)}"><input type="submit" value="Go" id="csearch_submit">
				</div>
				<input type="hidden" name="queryType" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}"/>
				<input type="hidden" name="container" 
					value='${briefObject.id}'/>
			</form>
			<div class="clear"></div>
			<p class="full_record_browse">
				<c:url var="collectionResultsUrl" scope="page" value='list/${briefObject.id}'></c:url>
				<a href="<c:out value='${collectionResultsUrl}' />">Browse&nbsp;(<c:out value="${childCount}"/> items)</a> or
				<a href="<c:out value='${structureUrl}' />">
					View ${fn:toLowerCase(briefObject.resourceType)} structure
				</a>
			</p>
		</div>
	</div>
</div>
<div class="lightest">
	<div class="fourcol lightest shadowtop">
		<div id="facetList" class="contentarea">
			<c:set var="selectedContainer" scope="request" value="${briefObject}"/>
			<h2>Contents</h2>
			<c:import url="/jsp/util/facetList.jsp">
			</c:import>
		</div>
	</div>
	<div class="threecol white shadowtop">
		<div class="contentarea">
			<c:import url="fullRecord/metadataBody.jsp" />
			
			<div id="hierarchical_view_full_record">
				<h2>Folder Browse View (or <a href="<c:out value="${structureUrl}" />">switch to structure browse</a>)</h2>
				<div class="structure" data-pid="${briefObject.id}">
				</div>
			</div>
			<br/>
			<c:import url="fullRecord/exports.jsp" />
		</div>
	</div>
	
</div>