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
<c:set var="defaultWebData" value="${briefObject.defaultWebData}"/>

<div class="onecol full_record_top">
	<div class="contentarea">
		<c:set var="thumbUrl">
			<c:choose>
				<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'THUMB_LARGE', briefObject) && 
						(briefObject.resourceType == searchSettings.resourceTypeFolder || briefObject.resourceType == searchSettings.resourceTypeCollection)}">
					${cdr:getDatastreamUrl(briefObject, 'THUMB_LARGE', fedoraUtil)}
				</c:when>
				<c:when test="${briefObject.resourceType == searchSettings.resourceTypeFolder}">
					/static/images/placeholder/large/folder.png
				</c:when>
				<c:when test="${briefObject.resourceType == searchSettings.resourceTypeCollection}">
					/static/images/placeholder/large/collection.png
				</c:when>
				<c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile 
						|| (briefObject.resourceType == searchSettings.resourceTypeAggregate && defaultWebData != null)}">
					/static/images/placeholder/large/${defaultWebData.extension}.png
				</c:when>
				<c:otherwise>
					/static/images/placeholder/large/blank.png
				</c:otherwise>
			</c:choose>
		</c:set>
		
		<a class="thumb_link large thumb_container">
			<img id="thumb_main" class="largethumb" src="${thumbUrl}"/>
			<span><img src="/static/images/lockedstate_large.gif"/></span>
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
			
			<ul class="pipe_list smaller">
				<c:if test="${defaultWebData != null}">
					<li><span class="bold">File Type:</span> <c:out value="${defaultWebData.extension}" /></li>
					<li><c:if test="${defaultWebData.filesize != null && defaultWebData.filesize != -1}"><span class="bold">Filesize:</span> <c:out value="${defaultWebData.filesize}"/></c:if></li>
				</c:if>
				<c:if test="${not empty briefObject.dateAdded}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_ADDED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></li></c:if>
				<c:if test="${not empty briefObject.dateCreated}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_CREATED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></li></c:if>
			</ul>
			<br class="clear"/>
			<p class="smaller">
				<c:set var="facetNodes" scope="request" value="${briefObject.path.facetNodes}"/>
				<span class="bold">Path:&nbsp;</span>
				<c:import url="common/hierarchyTrail.jsp">
					<c:param name="fieldKey"><c:out value="${'ANCESTOR_PATH'}"/></c:param>
					<c:param name="linkLast"><c:choose><c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">false</c:when><c:otherwise>true</c:otherwise></c:choose></c:param>
					<c:param name="limitToContainer">true</c:param>
				</c:import>
			</p>
			
			<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
				<c:param name="target" value="${currentAbsoluteUrl}" />
			</c:url>
			<c:if test="${empty pageContext.request.remoteUser}">
				<div class="actionlink left">
					<a href="<c:out value='${loginUrl}' />">Log in</a>
				</div>
			</c:if>
			<div class="actionlink left">
				<a href="/requestAccess/${briefObject.pid.pid}">Request Access</a>
			</div>
			
			<c:if test="${briefObject['abstractText'] != null}">
				<p class="clear">
					<c:out value="${briefObject['abstractText']}" />
				</p>
			</c:if>
		</div>
	</div>
</div>

<div class="onecol shadowtop">
	<div class="contentarea">
		<h2>Access by request</h2>
		<p>
			To access the contents of this item you will need to
			<c:if test="${empty pageContext.request.remoteUser}">
				<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
					<c:param name="target" value="${currentAbsoluteUrl}" />
				</c:url>
				<a href="<c:out value='${loginUrl}' />">log in</a> or
			</c:if>
			<a href="/requestAccess/${briefObject.pid.pid}">request access</a>.
		</p>
	</div>
</div>

<c:if test="${briefObject.resourceType == searchSettings.resourceTypeFile || briefObject.resourceType == searchSettings.resourceTypeAggregate}">
	<c:import url="fullRecord/neighborList.jsp" />
</c:if>
